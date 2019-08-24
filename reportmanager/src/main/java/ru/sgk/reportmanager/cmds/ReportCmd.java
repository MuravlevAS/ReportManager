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
import ru.sgk.reportmanager.data.SQLManager;

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
				if (args[0].equalsIgnoreCase("-mylist"))
				{
					List<Report> reportList = null;

					if (!hasPermission(sender, "reportmanager.usr.list"))
					{
						sender.sendMessage("§cНедостаточно прав.");
						return true;
					}
						if (args.length == 2)
						{
							try
							{
								reportList = SQLManager.Requests.getPlayerReports(sender.getName(), Integer.parseInt(args[1]));
							}
							catch (NumberFormatException e)
							{
								sender.sendMessage("§cНеправильный аргумент. Использование команды: §f/report -mylist [страница]");
								return true;
							}
						}
						else if (args.length == 1)
						{
							reportList = SQLManager.Requests.getPlayerReports(sender.getName(), 1);
						}
					

					Report.printReportList(sender, reportList);
				}
				else if (args[0].equalsIgnoreCase("-list"))
				{
					
					List<Report> reportList = null;
					if (!hasPermission(sender, "reportmanager.admin"))
					{
						sender.sendMessage("§cНедостаточно прав.");
						return true;
					}
					if (args.length == 2)
					{
						try
						{
							reportList = SQLManager.Requests.getReports(Integer.parseInt(args[1]));
						}
						catch (NumberFormatException e)
						{
							sender.sendMessage("§cНеправильный аргумент. Использование команды: §f/report -list [страница]");
							return true;
						}
					}
					else if (args.length == 1)
					{
						reportList = SQLManager.Requests.getReports(1);
					}
					Report.printReportList(sender, reportList);
					return true;
				}
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
							boolean responded = SQLManager.Requests.sendResponse(id, s, sender.getName());
							if (responded)
							{
								 
								sender.sendMessage(Configuration.getString(config, "messages.response-sended").replaceAll("%id%", id+""));
									Report.notifyPlayer(id);

							}
							else sender.sendMessage(Configuration.getString(config, "messages.wrong-id").replaceAll("%id%", args[1]));
						}
						catch (Exception e) {sender.sendMessage("§cid должен содержать только целые числа!");}
						return true;
					}
					else 
					{
						sender.sendMessage("§cИспользование команды: /report -reply <id> <Ответ>");
					}
				}
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
						Report report = SQLManager.Requests.getReport(id);
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
					long id = SQLManager.Requests.sendReport(sender.getName(), args[0], texts, toPlayer);
					sender.sendMessage(Configuration.getString(config, "messages.report-sended").replaceAll("%id%", "" + id));
					Report.notifyAdmin(id);
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
