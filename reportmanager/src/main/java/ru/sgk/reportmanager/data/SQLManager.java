package ru.sgk.reportmanager.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ru.sgk.reportmanager.ReportManager;

public class SQLManager 
{
	private static Connection connection;
	public static class Requests
	{
		/**
		 * Creates table with following fields: <br> 
		 * <b>id</b> - id of report<br>
		 * <b>responded</b> - was the repors responded or not <br>
		 * <b>reporter_player_name </b>- name of player that sends report <br>
		 * <b>reported_player_name</b> - name of player to whom report sends or theme of report<br>
		 * <b>to_player</b> if report about player<br>
		 * <b>responder</b> - name of admin that replied on report (null if report is not replied)<br>
		 * <b>response</b> - text of response<br>
		 * <b>text</b> - text of report<br>
		 */
		public static void createTable()
		{
			PreparedStatement ps = getStatement(
					  "CREATE TABLE IF NOT EXISTS `reportmanager`("
					+ "`id` INT(8) PRIMARY KEY AUTO_INCREMENT,"
					+ "`responded` BOOLEAN DEFAULT FALSE,"
					+ "`reporter_player_name` VARCHAR(255),"
					+ "`reported_player_name` VARCHAR(255),"
					+ "`to_player` BOOLEAN DEFAULT TRUE,"
					+ "`responder` VARCHAR(255),"
					+ "`response` TEXT,"
					+ "`checked` BOOLEAN DEFAULT FALSE,"
					+ "`text` TEXT) Engine=InnoDB DEFAULT CHARSET=utf8;");
			try 
			{
				ps.execute();
				ps.close();
			}
			catch (SQLException e) 
			{
				e.printStackTrace();
			}
		}
		
		/**
		 * Gets reports of specific player 
		 * @param playername - name of player
		 * @param index - index of page (one page is 5 reports)
		 * @return list of reports of player, or null or empty list if player have no reports 
		 */
		public static List<Report> getPlayerReports(String playername, int index)
		{
			PreparedStatement ps = getStatement("SELECT * FROM `reportmanager` WHERE `reporter_player_name` = ? ORDER BY `id` DESC limit ?, 5");
			try 
			{
				ps.setString(1, playername);
				ps.setInt(2, index-1);
				ps.execute();
				ResultSet rs = ps.getResultSet();

				List<Report> reportList = new ArrayList<Report>();
				
				while (rs.next())
				{
					long id = rs.getInt("id");
					String reporter = rs.getString("reporter_player_name");
					String reported = rs.getString("reported_player_name");
					boolean responded = rs.getBoolean("responded");
					String text = rs.getString("text");
					boolean toPlayer = rs.getBoolean("to_player");
					String response = rs.getString("response");
					String responder = rs.getString("responder");
					
					reportList.add(new Report(id, responded, reporter, reported, toPlayer, text, response, responder));
				}
				
				rs.close();
				ps.close();
				return index != 1 && reportList.isEmpty() ? null : reportList;
			}
			catch (SQLException e) { e.printStackTrace(); }
			return null;
			
		}

		/**
		 * Sends response to report of specific id
		 * @param id - id of report
		 * @param str - string to append
		 * @return false if exist no reports with ID <i>id</i>. Otherwise returns true
		 */
		public static boolean sendResponse(int id, String str, String responder)
		{
			try 
			{
				PreparedStatement ps = getStatement("UPDATE `reportmanager` SET `response` = ?, `responder` = ?, `responded` = TRUE, `checked` = FALSE where id = ?");
				ps.setString(1, str);
				ps.setString(2, responder);
				ps.setInt(3, id);
				int upd = ps.executeUpdate();
				ps.close();
				return upd > 0;
			}
			catch (SQLException e) 
			{
			}
			return false;
		}
		
		/**
		 * Sends respons of report
		 * @param id - id of report
		 * @param reporter name of player that sends report
		 * @param reported name of player to that report sends or theme of report
		 * @param text text of report
		 */
		public static long sendReport(String reporter, String reported, List<String> text, boolean toPlayer)
		{
			
			PreparedStatement ps = getStatement("INSERT INTO `reportmanager`(`reporter_player_name`, `reported_player_name`, `text`, `to_player`) VALUES (?, ?, ?, ?)");
			
			StringBuilder sb = new StringBuilder();
			for (String s : text)
			{
				sb.append(s + "\n");
			}
			
			try 
			{
				ps.setString(1, reporter);
				ps.setString(2, reported);
				ps.setString(3, sb.toString());
				ps.setBoolean(4, toPlayer);
				ps.execute();
				ps.close();
				ps = getStatement("SELECT * FROM `reportmanager` WHERE `reporter_player_name` = ? ORDER BY id DESC LIMIT 1");
				ps.setString(1, reporter);
				ps.execute();
				ResultSet rs = ps.getResultSet();
				rs.next();
				int i = rs.getInt("id");
				rs.close();
				ps.close();
				return i;
			}
			catch (SQLException e) 
			{
				e.printStackTrace();
			}
			return -1;
		}
		public static boolean checkReport(long id)
		{
			PreparedStatement ps = getStatement("UPDATE `reportmanager` SET `checked` = TRUE WHERE `id` = ?");
			try {
				ps.setLong(1, id);
				ps.execute();
				return true;
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return false;
		}
		public static Report getReport(long id)
		{
			PreparedStatement ps = getStatement("SELECT * FROM `reportmanager` WHERE id = ?");
			ResultSet rs = null;
			try 
			{
				ps.setLong(1, id);
				ps.execute();
				rs = ps.getResultSet();
				if (rs.next())
				{
					String reporter = rs.getString("reporter_player_name");
					String reported = rs.getString("reported_player_name");
					boolean responded = rs.getBoolean("responded");
					String text = rs.getString("text");
					boolean toPlayer = rs.getBoolean("to_player");
					String response = rs.getString("response");
					String responder = rs.getString("responder");
					boolean checked = rs.getBoolean("checked");
					

					rs.close();
					ps.close();	
					Report report = new Report(id, responded, reporter, reported, toPlayer, text, response, responder);
					report.setChecked(checked);
					return report;
				}
				
			}
			catch (SQLException e) 
			{
				e.printStackTrace();
			}return null;
		}
		
		/** 
		 * @param index - index of page
		 * @return List of reports of some page. List contains only non-responded values
		 */
		public static List<Report> getReports(int index)
		{
			int from = (index-1)*5;
			PreparedStatement ps = getStatement("SELECT * FROM `reportmanager` WHERE `responded` = FALSE ORDER BY id LIMIT ?, 5");
			ResultSet rs = null;
			try 
			{
				ps.setInt(1, from);
				ps.execute();
				rs = ps.getResultSet();
				List<Report> reportList = new ArrayList<Report>();
				
				while (rs.next())
				{
					long id = rs.getInt("id");
					String reporter = rs.getString("reporter_player_name");
					String reported = rs.getString("reported_player_name");
					boolean responded = rs.getBoolean("responded");
					String text = rs.getString("text");
					boolean toPlayer = rs.getBoolean("to_player");
					String response = rs.getString("response");
					String responder = rs.getString("responder");
					
					reportList.add(new Report(id, responded, reporter, reported, toPlayer, text, response, responder));
				}
				
				rs.close();
				ps.close();
				
				return index != 1 && reportList.isEmpty() ? null : reportList;
				
			}
			catch (SQLException e) 
			{
				e.printStackTrace();
			}
			return null;
		}
	}
	/**
	 * @return true if connection established
	 */
	public static boolean isConnected() 
	{ 
		return connection != null;
	}
	/**
	 * connect to database
	 * @param host - host
	 * @param database - database name
	 * @param user - username
	 * @param password - password of database user
	 */
	public static void connect(String host, String database, String user, String password)
	{
		if (!isConnected())
		{
			try {
				connection  = DriverManager.getConnection("jdbc:mysql://"+host+":3306/"+database+"?autoReconnect=true",user,password);
				ReportManager.log("§aConnection with database succesful complete");
			} catch (SQLException e) {
				ReportManager.log("§cCannot connect to database: ");
				System.out.println(e.getMessage());
			}
		}
	}
	/**
	 * closes database connection
	 */
	public static void closeConnection()
	{
		if (isConnected())
		{
			try {
				connection.close();
				connection = null;
			} catch (SQLException e) {
	
				ReportManager.log("§cError with close the connectoin:");
				System.out.println(e.getMessage());
			}
		}
	}
	public static PreparedStatement getStatement(String sql)
	{
		PreparedStatement statement;
		try {
			statement = connection.prepareStatement(sql);
			return statement;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
