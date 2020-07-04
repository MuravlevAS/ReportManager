package ru.sgk.reportmanager.bungee;

import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

import ru.sgk.reportmanager.data.Report;

public class Messenger implements PluginMessageListener{

	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] message) 
	{
		if (channel.equals("BungeeCord"))
		{
			ByteArrayDataInput in = ByteStreams.newDataInput(message);
			
			String subchannel = in.readUTF();
			if (subchannel.equalsIgnoreCase("report.send"))
			{
				long id = in.readLong();
				Report.notifyAdmin(id);
			}
			else if (subchannel.equalsIgnoreCase("report.reply"))
			{
				long id = in.readLong();
				Report.notifyPlayer(id);	
			}
		}
		
	}

}
