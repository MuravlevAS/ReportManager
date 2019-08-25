package ru.sgk.reportmanager.events;

import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import ru.sgk.reportmanager.data.Report;
import ru.sgk.reportmanager.data.MySQLManager;

public class MainEvents implements Listener {

	@EventHandler
	public  void onPlayerJoin(PlayerJoinEvent e)
	{
		Runnable task = () ->
		{
			List<Report> playerReportList = MySQLManager.Requests.getPlayerReports(e.getPlayer().getName(), 1);
			for (Report report : playerReportList)
			{
				if (report.isResponded() && !report.isChecked())
				{
					Report.notifyPlayer(e.getPlayer());
				}
			}
		};
		new Thread(task).start();
	}
}
