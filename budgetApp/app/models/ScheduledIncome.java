package models;

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
	 * @param date_next - the date the income was most recently created on 
	 * @param period - the number of days between payment recurrence  
	 */
	
	public ScheduledIncome (String date_next, String period) {
		try {
			this.date_next = new SimpleDateFormat("yyyy-MM-dd").parse(date_next);
		} catch (ParseException e) {
			this.date_next = null;
			e.printStackTrace();
		}
		
		this.period = Long.parseLong(period);
	}
	
	/**
	 * adds scheduled income to the db
	 * 
	 * @param scheduledIncome
	 * @return scheduledIncome Id
	 */
	
	public static long add(ScheduledIncome scheduledIncome) {
		Connection connection = DB.getConnection();
		PreparedStatement ps1 = null;
		ResultSet rs = null;
		int generatedKeys = 0;
		
		
		try {
			// insert values into DB
			ps1 = connection.prepareStatement("INSERT INTO scheduled_incomes (date_next, period) VALUES (?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
			ps1.setDate(1, new java.sql.Date(scheduledIncome.date_next.getTime()));
			ps1.setLong(2, scheduledIncome.period);
			
			// get the id to return
			generatedKeys = ps1.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (ps1 != null) {
					ps1.close();
				}
				if (connection != null) {
					connection.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		return generatedKeys;
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
		ResultSet rs = null;
		ResultSet rs2 = null;
		ResultSet rs3 = null;
		try {
			
			// we select the income schedulers who's next scheduled payment time has elapsed
			ps1 = connection.prepareStatement("SELECT * FROM scheduled_incomes WHERE date_next < ?");
			ps1.setDate(1, new java.sql.Date(currentDate.getTime()));
			rs = ps1.executeQuery();
			while (rs.next()) {
				long id = rs.getLong("id");
				
				long dbPeriod = rs.getLong("period");
				Date dbDate = rs.getDate("date_next");
				
				Calendar nextDate = Calendar.getInstance();
				nextDate.setTime(dbDate);
				
				// fetch the details of an income associated with the scheduler  
				ps2 = connection.prepareStatement("SELECT * FROM incomes WHERE scheduler = ?");
				ps2.setLong(1, id);
				rs2 = ps2.executeQuery();
				
				if (rs2.next()) { 
					// populate strings with result so we can initialise a new income
					String incomeOwner = String.valueOf(rs2.getLong("owner"));
					String incomeAmount = rs2.getBigDecimal("amount").toString();
					String incomeDescription = rs2.getString("description");
					
					// get links from tag - income link table
					long incomeId = rs2.getLong("id");
					
					ps3 = connection.prepareStatement("SELECT * FROM incomes_tags_map WHERE income = ?");
					ps3.setLong(1, incomeId);
					rs3 = ps3.executeQuery();
					
					// get all linked tags
					ps4 = connection.prepareStatement("SELECT * FROM incomes_tags WHERE id IN (?)");
					
					while (rs3.next()) { 
						ps4.setLong(1, rs3.getLong("tag"));
					}
					
					rs3 = ps4.executeQuery();
					
					String incomeTags = new String();
					
					// aggregate tag names in a string for the Income constructor
					while (rs3.next()) {
						incomeTags = incomeTags.concat(rs3.getString("name"));
						incomeTags = incomeTags.concat(",");
					}
					
					// trim excess comma
					incomeTags = incomeTags.substring(0, (incomeTags.length() - 1));
					
					// translate period flag into a month or number of days
					int days = 0;
					int months = 0;
					if (dbPeriod == 1) {
						days = 1;
						months = 0;
					} else if (dbPeriod == 2) {
						days = 7;
						months = 0;
					} else if (dbPeriod == 3) {
						days = 14;
						months = 0;
					} else if (dbPeriod == 4) {
						days = 0;
						months = 1;
					} else {
						days = 0;
						months = 0;
					}
					
					Income currIncome = null;
					
					// add the period to the next date the scheduled transaction is to be made until we have covered all the scheduled transactions
					while (nextDate.getTime().getTime() < currentDate.getTime() ) {
						System.out.println("addedexpense");
						currIncome = new Income(incomeOwner, incomeAmount, incomeTags,
								new SimpleDateFormat("yyyy-MM-dd").format(nextDate.getTime()), incomeDescription, id);
						
						Income.add(currIncome);
						System.out.println("months" + months + "days" + days);
						System.out.println(nextDate.getTime().toString());
						nextDate.add(Calendar.DATE, (int) days);
						nextDate.add(Calendar.MONTH, (int) months);
						System.out.println(nextDate.getTime().toString());
					}
				}
				
				// update date in scheduled incomes table
				ps1 = connection.prepareStatement("UPDATE scheduled_incomes SET date_next = ? WHERE id = ?");
				ps1.setDate(1, new java.sql.Date(nextDate.getTime().getTime()));
				ps1.setLong(2, id);
				ps1.executeUpdate();
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