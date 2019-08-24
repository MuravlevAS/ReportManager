package ru.sgk.reportmanager.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import ru.sgk.reportmanager.ReportManager;

public class Configuration 
{
	public static FileConfiguration load(String fileName)
	{
		File file = new File(ReportManager.getInstance().getDataFolder() + "/"+fileName);
		FileConfiguration config = null;
		try (	InputStream in = ReportManager.class.getResourceAsStream("/" + fileName);
				Reader reader = new InputStreamReader(in)) 
		{
		    config = YamlConfiguration.loadConfiguration(file);
			config.setDefaults(YamlConfiguration.loadConfiguration(reader));
			
			config.options().copyDefaults(true);
			saveConfig(config, fileName);
		}
		catch (IOException e) { e.printStackTrace(); }
		return config;
	}
	public static List<String> getListString(FileConfiguration config, String path)
	{
		List<String> tmp = config.getStringList(path);
		List<String> slist = new ArrayList<>();
		for (String s : tmp)
		{
			slist.add(s.replaceAll("&", "§"));
		}
		return slist;
	}
	
	public static void saveDefaultConfig()
	{
		saveConfig(ReportManager.getInstance().getConfig(), "config.yml");
	}
	
	public static void saveConfig(FileConfiguration config, String filename)
	{
		try {
			config.save(new File(ReportManager.getInstance().getDataFolder() + "/"+filename));
		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
	public static String getString(FileConfiguration config, String path)
	{
		return config.getString(path).replaceAll("&", "§");
	}
	public static void reload(String fileName, FileConfiguration config)
	{
		try
		{
			File file = new File(ReportManager.getInstance().getDataFolder() + "/"+ fileName);
			config.load(file);
			config.options().copyDefaults(true);
			saveConfig(config, fileName);
		}
		catch (FileNotFoundException e){} 
		catch (IOException e) {} 
		catch (InvalidConfigurationException e) {}
	}
	
}
