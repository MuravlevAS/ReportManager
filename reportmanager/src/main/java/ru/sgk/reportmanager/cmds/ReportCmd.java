package ru.sgk.reportmanager.cmds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import ru.sgk.reportmanager.ReportManager;
import ru.sgk.reportmanager.data.Configuration;
import ru.sgk.reportmanager.data.Report;
import ru.sgk.reportmanager.data.MySQLManager;

public class ReportCmd implements CommandExecutor 
{
	private FileConfiguration config;
	public ReportCmd(FileConfiguration config)
	{
		this.config = config;
	}
	/**
	 * permissions: <br>
	 * <b>reportmanager.dev</b> gives all permissions of this plugin <br>
	 * <b>reportmanager.admin</b> gives all permissions for commands .list, .reply for admins<br>
	 * <b>reportmanager.usr.report</b> gives permissions for player to sending reports<br>
	 * <b>reportmanager.usr.list</b> gives permissions for player to chech his list of reports<br>
	 * <b>reportmanager.usr.get</b> allows player to get his/her report
	 * @param sender - command sender
	 * @param permission - permission
	 * @return
	 */
	public static boolean hasPermission(CommandSender sender, String permission)
	{
		return sender.hasPermission("reportmanager.dev") || sender.hasPermission(permission);
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		
		if (cmd.getName().equalsIgnoreCase("report"))
		{
			
			if (args.length >= 1)
			{
				// sends to player list of all his reports
				if (args[0].equalsIgnoreCase("-mylist"))
				{

					if (!hasPermission(sender, "reportmanager.usr.list"))
					{
						sender.sendMessage("§cНедостаточно прав.");
						return true;
					}
					Runnable task = () ->
					{
						List<Report> reportList = null;
						if (args.length == 2)
						{
							try
							{
								reportList = MySQLManager.Requests.getPlayerReports(sender.getName(), Integer.parseInt(args[1]));
							}
							catch (NumberFormatException e)
							{
								
								sender.sendMessage("§cНеправильный аргумент. Использование команды: §f/report -mylist [страница]");
								return;
							}
						}
						else if (args.length == 1)
						{
							reportList = MySQLManager.Requests.getPlayerReports(sender.getName(), 1);
						}
						Report.printReportList(sender, reportList);
					};
					new Thread(task).start();
					return true;
				}
				// Sends list of reports that were not responded.
				// Using only for admin.
				else if (args[0].equalsIgnoreCase("-list"))
				{
					
					if (!hasPermission(sender, "reportmanager.admin"))
					{
						sender.sendMessage("§cНедостаточно прав.");
						return true;
					}
					Runnable task = ()->
					{
						List<Report> reportList = null;
						if (args.length == 2)
						{
							try
							{
								reportList = MySQLManager.Requests.getReports(Integer.parseInt(args[1]));
							}
							catch (NumberFormatException e)
							{
								sender.sendMessage("§cНеправильный аргумент. Использование команды: §f/report -list [страница]");
								return;
							}
						}
						else if (args.length == 1)
						{
							reportList = MySQLManager.Requests.getReports(1);
						}
						Report.printReportList(sender, reportList);
					};
					new Thread(task).start();
					return true;
				}
				// Send response on report
				else if (args[0].equalsIgnoreCase("-reply"))
				{
					if (!sender.hasPermission("reportmanager.admin"))
					{
						sender.sendMessage("§cНедостаточно прав.");
						return true;
					}
					if (args.length >= 3)
					{
						String[] argss = Arrays.copyOfRange(args, 2, args.length);
						String s = ""; 
						for (String string : argss)
							s = s + string + " ";
						try 
						{
							int id = Integer.parseInt(args[1]);
							// If db request has update at least one line, send notification to the player who sended report
							// and response to admint that all was successful. Otherwise send admin 
							boolean responded = MySQLManager.Requests.sendResponse(id, s, sender.getName());
							if (responded)
							{
								 
								sender.sendMessage(Configuration.getString(config, "messages.response-sended").replaceAll("%id%", id+""));
									Report.notifyPlayer(id);
								// Notify player that send that report about respond on his report.
								for (Player player : Bukkit.getOnlinePlayers())
								{
									ByteArrayDataOutput out = ByteStreams.newDataOutput();
									
									out.writeUTF("report.reply");
									out.writeLong(id);
									
									player.sendPluginMessage(ReportManager.getInstance(), "BungeeCord", out.toByteArray());
									break;
								}
							}
							// send error-message
							else sender.sendMessage(Configuration.getString(config, "messages.wrong-id").replaceAll("%id%", args[1]));
						}
						// send message about bad number format.
						catch (Exception e) {sender.sendMessage("§cid должен содержать только целые числа!");}
						return true;
					}
					else 
					{
						// send usage if command was put not properly
						sender.sendMessage("§cИспользование команды: /report -reply <id> <Ответ>");
					}
				}
				// getting report
				else if (args[0].equalsIgnoreCase("-get"))
				{
					if (args.length < 2)
					{
						sender.sendMessage("§cНеверное аргументы!");
						sender.sendMessage("§fИспользование: /report -get <id>");
						return true;
					}
					try
					{
						int id = Integer.parseInt(args[1]);
						Report report = MySQLManager.Requests.getReport(id);
						// if player is not admin then, if he has permission, he can get ONLY his reports,
						// others will give not permission meessage   
						if (!hasPermission(sender, "reportmanager.admin"))
						{
							if (report.getReporterPlayerName().equals(sender.getName()))
								if (!hasPermission(sender, "reportmanager.usr.get"))
								{
									sender.sendMessage("§cНедостаточно прав для просмотра данной записи!");
									return true;
								}
						}
						if (report == null) 
						{
							sender.sendMessage(Configuration.getString(config, "messages.no-report").replaceAll("%id%", id+""));
							return true;
						}
						if (report.isToPlayer())
						{
							for (String s : Configuration.getListString(config, "format.player"))
							{
								String s1 = s.replaceAll("%id%", report.getId() + "")
									 .replaceAll("%playername%", report.getReportedPlayerName())
									 .replaceAll("%reporter%", report.getReporterPlayerName())
									 .replaceAll("%text%", report.getText().replace("\n", " "))
									 .replaceAll("%respond%",report.getRespond())
									 .replaceAll("%admin%", report.getResponder());
								
								sender.sendMessage(s1);
							}
						}
						else 
						{
							for (String s : Configuration.getListString(ReportManager.getInstance().getConfig(), "format.theme"))
							{
								s = s.replaceAll("%id%", report.getId() + "")
									 .replaceAll("%playername%", report.getReportedPlayerName())
									 .replaceAll("%reporter%", report.getReporterPlayerName())
									 .replaceAll("%text%", report.getText().replace("\n", " "))
									 .replaceAll("%respond%", report.getRespond())
									 .replaceAll("%admin%", report.getResponder());
								sender.sendMessage(s);
							}
						}
					}
					catch (NumberFormatException e)
					{
						sender.sendMessage("§Неправильно введёт id");
						return true;
					}
				}
				// perform report sending
				else if (args.length >= 2)
				{
					if (!hasPermission(sender, "reportmanager.usr.report"))
					{
						sender.sendMessage("§cНедостаточно прав!");
						return true;
					}
					boolean toPlayer = true; 
					if (args[0].startsWith(".")) {
						toPlayer = false;
						args[0] = args[0].substring(1, args[0].length());
					}
					String[] textArray = Arrays.copyOfRange(args, 1, args.length);
					String text = "";
					for (String s : textArray)
					{
						text += s + " ";
					}
					List<String> texts =  new ArrayList<String>();
					texts.add(text);
					long id = MySQLManager.Requests.sendReport(sender.getName(), args[0], texts, toPlayer);
					sender.sendMessage(Configuration.getString(config, "messages.report-sended").replaceAll("%id%", "" + id));
					Report.notifyAdmin(id);
					for (Player player : Bukkit.getOnlinePlayers())
					{
						ByteArrayDataOutput out = ByteStreams.newDataOutput();
						
						out.writeUTF("report.send");
						out.writeLong(id);
						
						player.sendPluginMessage(ReportManager.getInstance(), "BungeeCord", out.toByteArray());
						break;
					}
					return true;
				}
				else
				{
					sender.sendMessage(Configuration.getString(config, "messages.wrong-text"));
					return true;
				}
			}
			else
			{
				printUsage(sender);
			}
		}
		return true;
	}
	private void printUsage(CommandSender sender)
	{
		sender.sendMessage("§rHelp:");
		if (hasPermission(sender, "reportmanager.usr.report"))
		{
			sender.sendMessage("§f /report <ник игрока> <описание жалобы> - Отправить жалобу на игрока");
			sender.sendMessage("§f /report .<тема> <описание жалобы> - Отправить жалобу на определённую тему, к примеру: /report .баг происходит какой-то баг");
		}
		if (hasPermission(sender, "reportmanager.usr.list"))
			sender.sendMessage("§f /report -mylist [страница] - Посмотреть свои отправленные жалобы");
		if (hasPermission(sender, "reportmanager.usr.get") || hasPermission(sender, "reportmanager.admin") ) 
			sender.sendMessage("§f /report -get <id> - Посмотреть определённую жалобу");
		if (hasPermission(sender, "reportmanager.admin"))
		{
			sender.sendMessage("§f /report -list [страница] - Посмотреть новые жалобы пользователей");
			sender.sendMessage("§f /report -reply [id] - Ответить на жалобу по id [id]");
		}
	}
}
