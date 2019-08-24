package ru.sgk.reportmanager.cmds;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import ru.sgk.reportmanager.ReportManager;
import ru.sgk.reportmanager.data.Configuration;
import ru.sgk.reportmanager.data.SQLManager;

public class ReportManagerCmd implements CommandExecutor 
{

	public static boolean hasPermission(CommandSender sender, String permission)
	{
		return sender.hasPermission("reportmanager.dev") || sender.hasPermission(permission);
	}
	private FileConfiguration config;
	public ReportManagerCmd(FileConfiguration config)
	{
		this.config = config;
	}
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		if (cmd.getName().equalsIgnoreCase("reportmanager"))
		{
			if (!hasPermission(sender, "reportmanager.dev"))
			{
				sender.sendMessage("§cУ вас нет прав на выполнение данной команды");
				return true;
			}
			if (args.length == 1 && args[0].equalsIgnoreCase("reload"))
			{
				Configuration.reload("config.yml", config);
				SQLManager.closeConnection();
				ReportManager.dbConnect();
				sender.sendMessage("&aPlugin successfully reloaded!");
			}
			else printUsage(sender);
		}
		return true;
	}
	private void printUsage(CommandSender sender)
	{
		sender.sendMessage("§rHelp:");
		sender.sendMessage("§c/reportmanager reload §f- перезагрузить конфиг.");
	}

}
