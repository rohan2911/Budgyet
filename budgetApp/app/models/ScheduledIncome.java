package models;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import play.db.DB;

/**
 * ScheduledIncome
 * 
 * Creates Income instances based on a user specified date and repetition period.
 * Invoked by the Schedule Actor on a period specified in Global
 *
 */

public class ScheduledIncome {
	public Date date_next;
	public long period;
	
	/**
	 * Constructor
	 * 
	 * @param date_last - the date the income was most recently created on 
	 * @param period - the number of days between payment reccurrences  
	 */
	
	public ScheduledIncome (String date_last, String period) {
		try {
			this.date_next = new SimpleDateFormat("yyyy-MM-dd").parse(date_last);
		} catch (ParseException e) {
			this.date_next = null;
			e.printStackTrace();
		}
		
		this.period = Long.getLong(period);
	}
	
	
	
	public static boolean add(ScheduledIncome scheduledIncome) {
		
		return false;
	}
	
	/**
	 * 
	 * Scheduled Task is run daily to ensure that scheculed incomes are added periodically
	 * on a user specified period given on creation.
	 * 
	 * @return SQL errors (actually nothing atm lol)
	 */
	
	public static String scheduledTask() {
		Connection connection = DB.getConnection();
		
		// getInstance() intialises a Calendar with the current system date and time
		Calendar currentInstance = Calendar.getInstance();
		Date currentDate = currentInstance.getTime();
		PreparedStatement ps1 = null;
		PreparedStatement ps2 = null;
		PreparedStatement ps3 = null;
		PreparedStatement ps4 = null;
		PreparedStatement ps5 = null;
		ResultSet rs = null;
		ResultSet rs2 = null;
		ResultSet rs3 = null;
		try {
			
			// we select the income schedulers who's next scheduled payment time has elapsed
			ps1 = connection.prepareStatement("SELECT * FROM expenses_scheduled WHERE date_next < ?");
			ps1.setDate(1, new java.sql.Date(currentDate.getTime()));
			rs = ps1.executeQuery();
			
			while (!rs.isAfterLast()) {
				long id = rs.getLong("id");
				
				long dbPeriod = rs.getLong("period");
				Date dbDate = rs.getDate("date_next");
				
				Calendar nextDate = Calendar.getInstance();
				nextDate.setTime(dbDate);
				
				// fetch the details of an income associated with the scheduler  
				ps2 = connection.prepareStatement("SELECT * FROM expenses WHERE scheduler = ?");
				ps2.setLong(1, id);
				rs2 = ps2.executeQuery();
				
				// populate strings with result so we can initialise a new income
				String incomeOwner = String.valueOf(rs2.getLong("owner"));
				String incomeAmount = rs2.getBigDecimal("amount").toString();
				String incomeDescription = rs2.getString("description");
				
				// get links from tag - income link table
				long incomeId = rs2.getLong("id");
				String tags = new String();
				
				ps3 = connection.prepareStatement("SELECT * FROM incomes_tags_map WHERE income = ?");
				ps3.setLong(1, incomeId);
				rs3 = ps3.executeQuery();
				
				// get all linked tags
				ps4 = connection.prepareStatement("SELECT * FROM income_tags WHERE id IN (?)");
				
				while (!rs3.isAfterLast()) { 
					ps4.setLong(1, rs3.getLong("tag"));
					rs3.next();
				}
				
				rs3 = ps4.executeQuery();
				
				String incomeTags = new String();
				
				// aggregate tag names in a string for the Income constructor
				while (!rs3.isAfterLast()) {
					incomeTags = incomeTags.concat(rs3.getString("name"));
					incomeTags = incomeTags.concat(",");
					rs3.next();
				}
				
				// trim excess comma
				incomeTags = incomeTags.substring(0, (incomeTags.length() - 1));
				
				// translate period flag into a month or number of days
				int days = 0;
				int months = 0;
				switch ((int) dbPeriod) {
					case 1:
						days = 1;
					case 2:
						days = 7;
					case 3:
						days = 14;
					case 4:
						months = 1;
					default:
				}
				
				Income currIncome = null;
				
				// add the period to the next date the scheduled transaction is to be made until we have covered all the scheduled transactions
				while (nextDate.getTime().getTime() < currentDate.getTime() ) {
					
					currIncome = new Income(incomeOwner, incomeAmount, incomeTags,
							new SimpleDateFormat("yyyy-MM-dd").format(nextDate), incomeDescription);
					
					Income.add(currIncome);
					
					nextDate.add(Calendar.DATE, (int) days);
					nextDate.add(Calendar.MONTH, (int) months);
				}
				
				rs.updateDate("date_last", new java.sql.Date(nextDate.getTime().getTime()));
				rs.updateRow();
				rs.next();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (rs2 != null) {
					rs.close();
				}
				if (rs3 != null) {
					rs.close();
				}
				if (ps1 != null) {
					ps1.close();
				}
				if (ps2 != null) {
					ps2.close();
				}
				if (ps3 != null) {
					ps3.close();
				}
				if (ps4 != null) {
					ps4.close();
				}
				if (connection != null) {
					connection.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
}