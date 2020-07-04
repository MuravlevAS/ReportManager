package ru.sgk.reportmanager.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ru.sgk.reportmanager.ReportManager;

public class MySQLManager 
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
			sendRequest(
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
		}
		
		/**
		 * Gets reports of specific player 
		 * @param playername - name of player
		 * @param index - index of page (one page is 5 reports)
		 * @return list of reports of player, or null or empty list if player have no reports 
		 */
		public static List<Report> getPlayerReports(String playername, int index)
		{
			
			try (ResultSet rs = getResult("SELECT * FROM `reportmanager` WHERE `reporter_player_name` = ? ORDER BY `id` DESC limit ?, 5", playername, index-1))
			{

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
				return sendUpdate("UPDATE `reportmanager` SET `response` = ?, `responder` = ?, `responded` = TRUE, `checked` = FALSE where id = ?"
						, str, responder, id) > 0;
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
			StringBuilder sb = new StringBuilder();
			for (String s : text)
			{
				sb.append(s + "\n");
			}
				
				sendRequest("INSERT INTO `reportmanager`(`reporter_player_name`, `reported_player_name`, `text`, `to_player`) VALUES (?, ?, ?, ?)",
						reporter,
						reported,
						sb.toString(),
						toPlayer);
				
				try (ResultSet rs = getResult("SELECT * FROM `reportmanager` WHERE `reporter_player_name` = ? ORDER BY id DESC LIMIT 1", reporter))
				{
					rs.next();
					int i = rs.getInt("id");
					
					return i;
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			return -1;
		}
		public static boolean checkReport(long id)
		{
			if (sendUpdate("UPDATE `reportmanager` SET `checked` = TRUE WHERE `id` = ?", id) > 0)
				return true;
			return false;
		}
		public static Report getReport(long id)
		{
			
			
			try (ResultSet rs = getResult("SELECT * FROM `reportmanager` WHERE id = ?", 1)) 
			{
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
					

					Report report = new Report(id, responded, reporter, reported, toPlayer, text, response, responder);
					report.setChecked(checked);
					return report;
				}
				
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return null;
		}
		
		/** 
		 * @param index - index of page
		 * @return List of reports of some page. List contains only non-responded values
		 */
		public static List<Report> getReports(int index)
		{
			int from = (index-1)*5;
			
			
			try (ResultSet rs = getResult("SELECT * FROM `reportmanager` WHERE `responded` = FALSE ORDER BY id LIMIT ?, 5", from)) 
			{
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
	 * Sends any request to db
	 * @param request - request
	 */
	private static void sendRequest(String request, Object... args)
	{
		try(PreparedStatement ps = getStatement(request))
		{
			for (int i = 0; i < args.length; i++)
			{
				ps.setObject(i+1, args[i]);
			}
			ps.execute();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * Send here only request such as INSERT, UPDATE or DELETE.
	 * @param request - request 
	 * @return either (1) the row count for SQL Data Manipulation Language (DML) statementsor (2) 0 for SQL statements that return nothing
	 */
	private static int sendUpdate(String request, Object... args)
	{
		try(PreparedStatement ps = getStatement(request))
		{
			for (int i = 0; i < args.length; i++)
			{
				ps.setObject(i+1, args[i]);
			}
			return ps.executeUpdate();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}
	/**
	 * <b>Note:</b> always call method ResultSet.close() in the end of your code with this method, or using Autocloselable (try-with-resources) statement 
	 * @param request - request
	 * @return ResultSet which correspond to request; 
	 */
	private static ResultSet getResult(String request, Object... args) throws SQLException  
	{
		try(PreparedStatement ps = getStatement(request))
		{
			for (int i = 0; i < args.length; i++)
			{
				ps.setObject(i+1, args[i]);
			}
			return ps.executeQuery();
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
