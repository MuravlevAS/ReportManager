package ru.sgk.reportmanager.data;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import ru.sgk.reportmanager.ReportManager;
import ru.sgk.reportmanager.cmds.ReportCmd;

public class Report {
	private long id;
	private boolean responsed;
	private String reporterPlayerName;
	private String reportedPlayerName;
	private boolean toPlayer = true;
	private String text;
	private String respond;
	private String responder;
	private boolean checked = false;
	
	public Report(long id, boolean responsed, String reporterPlayerName, String reportedPlayerName, boolean toPlayer,
			String text, String respond, String responder) {
		super();
		this.id = id;
		this.responsed = responsed;
		this.reporterPlayerName = reporterPlayerName;
		this.reportedPlayerName = reportedPlayerName;
		this.toPlayer = toPlayer;
		this.text = text;
		this.respond = respond;
		this.responder = responder;
	}
	
	public long getId() 
	{
		return id;
	}
	
	public boolean isResponded() 
	{
		return responsed;
	}
	
	public String getReporterPlayerName() 
	{
		return reporterPlayerName;
	}
	
	public String getReportedPlayerName() 
	{
		return reportedPlayerName;
	}
	
	public String getText() 
	{
		return text;
	}
	
	public boolean isToPlayer() 
	{
		return toPlayer;
	}

	public String getRespond() 
	{
		return respond == null ? "Нет ответа" : respond;
	}

	public String getResponder() 
	{
		return responder == null ? "Нет ответа" : responder;
	}
	
	public boolean isChecked() 
	{
		return checked;
	}

	public void setChecked(boolean checked) {
		this.checked = checked;
	}

	public static void printReportList(CommandSender sender, List<Report> reportList)
	{
		try
		{
			if (reportList.isEmpty() || reportList.size() < 1)
			{
				sender.sendMessage(Configuration.getString(ReportManager.getInstance().getConfig(), "variables.no-reports"));
				return;
			}
		}
		catch (NullPointerException e)
		{
			sender.sendMessage("§cТакой страницы нет.");
			return;
		}
		
		sender.sendMessage("Жалобы: ");
		for (Report report : reportList)
		{
			if (report.isToPlayer())
			{
				for (String s : Configuration.getListString(ReportManager.getInstance().getConfig(), "format.player"))
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
			if (!ReportCmd.hasPermission(sender, "reportmanager.admin"))
				SQLManager.Requests.checkReport(report.getId());
		}
		
	}
	public static void notifyAdmin(String reporterName)
	{
		Runnable task = () ->
		{
			Report report = SQLManager.Requests.getPlayerReports(reporterName, 1).get(0);
			long id = report.getId();
			for (Player player : Bukkit.getOnlinePlayers())
			{
				if (ReportCmd.hasPermission(player, "reportmanager.admin"))
				{
					player.sendMessage(Configuration.getString(ReportManager.getInstance().getConfig(), "messages.notify-admin")
							.replaceAll("%id%", id+""));
				}
			}
		};
		new Thread(task).start();
	}
	public static void notifyAdmin(long id)
	{	
		
		for (Player player : Bukkit.getOnlinePlayers())
		{
			if (ReportCmd.hasPermission(player, "reportmanager.admin"))
			{
				player.sendMessage(Configuration.getString(ReportManager.getInstance().getConfig(), "messages.notify-admin")
						.replaceAll("%id%", id+""));
			}
		}
	}
	public static void notifyPlayer(CommandSender player)
	{
		player.sendMessage(Configuration.getString(ReportManager.getInstance().getConfig(), "messages.notification"));
	}
	
	public static void notifyPlayer(long id)
	{
		Runnable task = () ->
		{
			String name = SQLManager.Requests.getReport(id).getReporterPlayerName();
			try
			{
				Player p = Bukkit.getPlayer(name);
				if (p.isOnline())
					p.sendMessage(Configuration.getString(ReportManager.getInstance().getConfig(), "messages.notification"));
			}
			catch (Exception e) {}
		};
		Bukkit.getScheduler().runTaskAsynchronously(ReportManager.getInstance(), task);
		
	}
	
	
}
