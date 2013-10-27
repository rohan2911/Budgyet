package models;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
	public int period;
	public long owner;
	public BigDecimal income_amount;
	public String income_description;
	public String tagName;
	
	/**
	 * Constructor
	 * 
	 * @param date_next - the date the Income was most recently created on 
	 * @param period - the number of days between payment recurrence  
	 */
	
	public ScheduledIncome (String date_next, String period, String Income_owner, String income_amount, String income_description, String tag) {
		try {
			this.date_next = new SimpleDateFormat("yyyy-MM-dd").parse(date_next);
		} catch (ParseException e) {
			this.date_next = null;
			e.printStackTrace();
		}
		this.period = Integer.parseInt(period);
		this.owner = Long.parseLong(Income_owner);
		this.income_amount = new BigDecimal(income_amount).setScale(2, RoundingMode.HALF_UP);
		this.income_description = income_description;
		this.tagName = tag;
	}
	
	/**
	 * adds scheduled Income to the db
	 * 
	 * @param scheduledIncome
	 * @return scheduledIncome Id
	 */
	
	public static long add(ScheduledIncome scheduledIncome) {
		Connection connection = DB.getConnection();
		PreparedStatement psInsExp = null;
		PreparedStatement psInsTag = null;
		PreparedStatement psTagId = null;
		ResultSet rsTagId = null;
		ResultSet rsSchKey = null;
		long schId = -1;
		
		try {

			// insert the tag
			psInsTag = connection.prepareStatement("insert into incomes_tags (owner, name) select * from (select ?, ?) as tmp "
					+ "where not exists (select 1 from incomes_tags where owner = ? and name = ?)");
			psInsTag.setLong(1, scheduledIncome.owner);
			psInsTag.setLong(3, scheduledIncome.owner);
			psInsTag.setString(2, scheduledIncome.tagName);
			psInsTag.setString(4, scheduledIncome.tagName);
			psInsTag.executeUpdate();
			
			// get the tag's id
			psTagId = connection.prepareStatement("select id from incomes_tags where owner = ? and name = ?");
			psTagId.setLong(1, scheduledIncome.owner);
			psTagId.setString(2, scheduledIncome.tagName);
			rsTagId = psTagId.executeQuery();

			// get the tag id, insert the scheduled income with this id
			if (rsTagId.next()) {
				long tagId = rsTagId.getLong(1);
				// insert the scheduled income
				psInsExp = connection.prepareStatement("INSERT INTO scheduled_incomes (date_next, period, owner, "
						+ "income_amount, income_description, tag) VALUES (?, ?, ?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
				psInsExp.setDate(1, new java.sql.Date(scheduledIncome.date_next.getTime()));
				psInsExp.setInt(2, scheduledIncome.period);
				psInsExp.setLong(3, scheduledIncome.owner);
				psInsExp.setBigDecimal(4, scheduledIncome.income_amount);
				psInsExp.setString(5, scheduledIncome.income_description);
				psInsExp.setLong(6, tagId);
				psInsExp.executeUpdate();
				
				rsSchKey = psInsExp.getGeneratedKeys();
				
				// process tags string (code pulled from Income add)
				if (rsSchKey.next()) {
					schId = rsSchKey.getLong(1);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (rsSchKey != null) {
					rsSchKey.close();
				}
				if (rsTagId != null) {
					rsTagId.close();
				}
				if (psInsExp != null) {
					psInsExp.close();
				}
				if (psInsTag != null) {
					psInsTag.close();
				}
				if (psTagId != null) {
					psTagId.close();
				}
				if (connection != null) {
					connection.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		return schId;
	}
	
	/**
	 * 
	 * Scheduled Task is run daily to ensure that scheculed Incomes are added periodically
	 * on a user specified period given on creation.
	 * 
	 * @return SQL errors (actually nothing atm lol)
	 */
	
	public static boolean scheduledTask() {
		Connection connection = DB.getConnection();
		
		// getInstance() intialises a Calendar with the current system date and time
		Calendar currentInstance = Calendar.getInstance();
		Date currentDate = currentInstance.getTime();
		PreparedStatement ps1 = null;
		ResultSet rs = null;
		try {
			
			// we select the Income schedulers who's next scheduled payment time has elapsed
			ps1 = connection.prepareStatement("SELECT * FROM scheduled_incomes WHERE date_next < ?");
			ps1.setDate(1, new java.sql.Date(currentDate.getTime()));
			rs = ps1.executeQuery();
			while (rs.next()) {
				implementSchedule(rs, connection, currentDate);
			}

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
		return true;
	}
	
	/**
	 * 
	 * Adds Incomes for a newly created scheduled Income 
	 * 
	 * @return success
	 */
	
	public static boolean init (long id) {
		Connection connection = DB.getConnection();
		
		// getInstance() intialises a Calendar with the current system date and time
		Calendar currentInstance = Calendar.getInstance();
		Date currentDate = currentInstance.getTime();
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			ps = connection.prepareStatement("SELECT * FROM scheduled_incomes WHERE id = ?");
			ps.setLong(1, id);
			rs = ps.executeQuery();
			
			if (rs.next()) {
				implementSchedule(rs, connection, currentDate);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (ps != null) {
					ps.close();
				}
				if (connection != null) {
					connection.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		return true;
	}
	
	/**
	 * 
	 * Using a scheduled Incomes details add the Incomes due for a schedule based on current system time
	 * the schedule is passed in as a ResultSet
	 * 
	 * @return success
	 */
	
	private static boolean implementSchedule(ResultSet rs, Connection connection, Date currentDate) {
		
		PreparedStatement ps1 = null;
		PreparedStatement ps2 = null;
		PreparedStatement ps3 = null;
		PreparedStatement ps4 = null;
		ResultSet rs2 = null;
		ResultSet rs3 = null;
		
		try {
			long id = rs.getLong("id");
			
			int dbPeriod = rs.getInt("period");
			Date dbDate = rs.getDate("date_next");
			long tagId = rs.getLong("tag");
			
			Calendar nextDate = Calendar.getInstance();
			nextDate.setTime(dbDate);
			
			// populate strings so we can initialise a new Income
			String incomeOwner = String.valueOf(rs.getLong("owner"));
			String incomeAmount = rs.getBigDecimal("income_amount").toString();
			String incomeDescription = rs.getString("income_description");
			
			
			// get all linked tags
			ps3 = connection.prepareStatement("SELECT * FROM incomes_tags WHERE id = ?");
			ps3.setLong(1, tagId);
			rs3 = ps3.executeQuery();
			if(rs3.next()) {
				String incomeTag = rs3.getString("name");
				
				
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
				while (nextDate.getTime().getTime() <= currentDate.getTime() ) {
					currIncome = new Income(incomeOwner, incomeAmount, incomeTag,
							new SimpleDateFormat("yyyy-MM-dd").format(nextDate.getTime()), incomeDescription, id);
					
					Income.add(currIncome);
					nextDate.add(Calendar.DATE, (int) days);
					nextDate.add(Calendar.MONTH, (int) months);
				}
				
				// update date in scheduled Incomes table
				ps4 = connection.prepareStatement("UPDATE scheduled_incomes SET date_next = ? WHERE id = ?");
				ps4.setDate(1, new java.sql.Date(nextDate.getTime().getTime()));
				ps4.setLong(2, id);
				ps4.executeUpdate();
			}

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (rs2 != null) {
					rs2.close();
				}
				if (rs3 != null) {
					rs3.close();
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
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		return true;
	}
	

	/**
	 *
	 * Gets the scheduler for a given income
	 * 
	 * @param the id of the income
	 * @return a Scheduled income populated with values from the database
	 */
	public static ScheduledIncome get(long incomeId) {
		ScheduledIncome returnScheduler = null; 
		Connection connection = DB.getConnection();
		
		PreparedStatement psIncomeSelect = null;
		PreparedStatement psSchedulerSelect = null;
		ResultSet rsIncome = null;
		ResultSet rsScheduler = null;
		
		try {
			// select the scheduler field of the record for the income id
			psIncomeSelect = connection.prepareStatement("SELECT scheduler FROM incomes WHERE id = ?");
			psIncomeSelect.setLong(1, incomeId);
			rsIncome = psIncomeSelect.executeQuery();
			
			long schedulerId = rsIncome.getLong(1);
			
			// select the schedueler object
			psSchedulerSelect = connection.prepareStatement("SELECT * FROM scheduled_incomes WHERE id = ?");
			psSchedulerSelect.setLong(1, schedulerId);
			rsScheduler = psSchedulerSelect.executeQuery();
			
			// set return value
			returnScheduler = new ScheduledIncome(rsScheduler.getString("date_next"), rsScheduler.getString("period"),
					rsScheduler.getString("owner"), rsScheduler.getString("income_amount"),
					rsScheduler.getString("income_description"), rsScheduler.getString("tag"));
			
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (rsIncome != null) {
					rsIncome.close();
				}
				
				if (rsScheduler != null) {
					rsScheduler.close();
				}
				
				if (psIncomeSelect != null) {
					psIncomeSelect.close();
				}
				
				if (psSchedulerSelect != null) {
					psSchedulerSelect.close();
				}
				
				if (connection != null) {
					connection.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}

		}
		
		return returnScheduler;
	}
}