package ru.sgk.reportmanager;

import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import ru.sgk.reportmanager.bungee.Messenger;
import ru.sgk.reportmanager.cmds.ReportCmd;
import ru.sgk.reportmanager.cmds.ReportManagerCmd;
import ru.sgk.reportmanager.data.Configuration;
import ru.sgk.reportmanager.data.MySQLManager;
import ru.sgk.reportmanager.events.MainEvents;

public class ReportManager extends JavaPlugin 
{
	private static Logger logger;
	private static String prefix = "§f[§cReport§fManager]";
	private static String debugPrefix = "§f[§cReport§fManager]§4[Debug]&r";
	private static ReportManager instance;
	private static FileConfiguration config;
	@Override
    public void onEnable() 
    {
		instance = this;
    	
		logger = Bukkit.getLogger();
		
    	config = Configuration.load("config.yml");

    	getServer().getMessenger().registerIncomingPluginChannel(instance, "BungeeCord", new Messenger());
    	getServer().getMessenger().registerOutgoingPluginChannel(instance, "BungeeCord");
    	
    	getCommand("report").setExecutor(new ReportCmd(config));
    	getCommand("reportmanager").setExecutor(new ReportManagerCmd(config));
    	dbConnect();
    	getServer().getPluginManager().registerEvents(new MainEvents(), instance);
    	log("plugin was enabled");
    }
    
	public static ReportManager getInstance() 
	{
		return instance;
	}
	/**
	 * sends the message to console with plugin prefix
	 * @param message
	 */
	public static void log(String message)
    {
    	logger.info(prefix + " §r"+ message);
    }
    
	/**
	 * sends debug message to console with debug message
	 * @param message
	 */
    public static void debug(String message)
    {
    	logger.info(debugPrefix + " §r"+ message);
    }
    
    @Override
    public void onDisable() 
    {
    	MySQLManager.closeConnection();
    	log("plugin was enabled");
    	Configuration.saveDefaultConfig();
    }

	/**
	 * @return the config
	 */
    @Override
	public FileConfiguration getConfig() {
		return config;
	}
    public static void dbConnect()
    {
    	
    	String host = config.getString("database.host");
    	String database = config.getString("database.database");
    	String user = config.getString("database.user");
    	String password = config.getString("database.password", "");
    	MySQLManager.connect(host, database, user, password);
    	MySQLManager.Requests.createTable();
    }
}
