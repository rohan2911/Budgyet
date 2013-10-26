package models;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
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
	public long period;
	public long owner;
	public BigDecimal income_amount;
	public String income_description;
	public List<String> tags;
	
	/**
	 * Constructor
	 * 
	 * @param date_next - the date the income was most recently created on 
	 * @param period - the number of days between payment recurrence  
	 */
	
	public ScheduledIncome (String date_next, String period, String income_owner, String income_amount, String income_description, String tags) {
		try {
			this.date_next = new SimpleDateFormat("yyyy-MM-dd").parse(date_next);
		} catch (ParseException e) {
			this.date_next = null;
			e.printStackTrace();
		}
		this.period = Long.parseLong(period);
		this.owner = Long.parseLong(income_owner);
		this.income_amount = new BigDecimal(income_amount).setScale(2, RoundingMode.HALF_UP);
		this.income_description = income_description;
		this.tags = new ArrayList<String>(Arrays.asList(tags.split(",")));
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
		PreparedStatement ps2 = null;
		PreparedStatement ps3 = null;
		PreparedStatement ps4 = null;
		ResultSet rs = null;
		ResultSet generatedKeys = null;
		long scheudled_income_id = 0;
		
		
		try {
			// insert values into DB
			ps1 = connection.prepareStatement("INSERT INTO scheduled_incomes (date_next, period, owner, income_amount, income_description) VALUES (?, ?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
			ps1.setDate(1, new java.sql.Date(scheduledIncome.date_next.getTime()));
			ps1.setLong(2, scheduledIncome.period);
			ps1.setLong(3, scheduledIncome.owner);
			ps1.setBigDecimal(4, scheduledIncome.income_amount);
			ps1.setString(5, scheduledIncome.income_description);
			ps1.executeUpdate();
			
			// get the id to return
			generatedKeys = ps1.getGeneratedKeys();
			
			// process tags string (code pulled from income add)
			if (generatedKeys.next()) {
				scheudled_income_id = generatedKeys.getLong(1);
				
				// insert the tags
				ps2 = connection.prepareStatement("insert into incomes_tags (owner, name) select * from (select ?, ?) as tmp where not exists (select 1 from incomes_tags where owner = ? and name = ?)");
				ps2.setLong(1, scheduledIncome.owner);
				ps2.setLong(3, scheduledIncome.owner);
				Iterator<String> tags_it = scheduledIncome.tags.iterator();
				while (tags_it.hasNext()) {
					String tag = tags_it.next();
					ps2.setString(2, tag);
					ps2.setString(4, tag);
					ps2.executeUpdate();
				}
				
				// get tag ids
				String sql3 = "select id from incomes_tags where owner = ? and (name = ?";
				for (int i=0; i<scheduledIncome.tags.size()-1; i++) {
					sql3 += " OR name = ?";
				}
				sql3 += ")";
				ps3 = connection.prepareStatement(sql3);
				ps3.setLong(1, scheduledIncome.owner);
				for (int i=0; i<scheduledIncome.tags.size(); i++) {
					ps3.setString(i+2, scheduledIncome.tags.get(i));
				}
				rs = ps3.executeQuery();
				
				// insert the income tag mapping
				ps4 = connection.prepareStatement("insert into scheduled_incomes_tags_map (scheduled_income, tag) values (?, ?)");
				ps4.setLong(1, scheudled_income_id);
				while (rs.next()) {
					ps4.setLong(2, rs.getLong("id"));
					ps4.executeUpdate();
				}
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
				if (generatedKeys != null) {
					generatedKeys.close();
				}
				if (connection != null) {
					connection.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		return scheudled_income_id;
	}
	
	/**
	 * 
	 * Scheduled Task is run daily to ensure that scheculed incomes are added periodically
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
			
			// we select the income schedulers who's next scheduled payment time has elapsed
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
	 * Adds incomes for a newly created scheduled income 
	 * 
	 * @return success
	 */
	
	public static boolean init (long id) {
		Connection connection = DB.getConnection();
		
		// getInstance() intialises a Calendar with the current system date and time
		Calendar currentInstance = Calendar.getInstance();
		Date currentDate = currentInstance.getTime();
		PreparedStatement ps1 = null;
		ResultSet rs = null;
		
		try {
			ps1 = connection.prepareStatement("SELECT * FROM scheduled_incomes WHERE id = ?");
			ps1.setLong(1, id);
			rs = ps1.executeQuery();
			
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
	 * Using a scheduled incomes details add the incomes due for a schedule based on current system time
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
			
			long dbPeriod = rs.getLong("period");
			Date dbDate = rs.getDate("date_next");
			
			Calendar nextDate = Calendar.getInstance();
			nextDate.setTime(dbDate);
			
			// populate strings so we can initialise a new income
			String incomeOwner = String.valueOf(rs.getLong("owner"));
			String incomeAmount = rs.getBigDecimal("income_amount").toString();
			String incomeDescription = rs.getString("income_description");
			
			
			ps2 = connection.prepareStatement("SELECT * FROM scheduled_incomes_tags_map WHERE scheduled_income = ?");
			ps2.setLong(1, id);
			rs2 = ps2.executeQuery();
			
			// get all linked tags
			ps3 = connection.prepareStatement("SELECT * FROM incomes_tags WHERE id IN (?)");
			
			while (rs2.next()) { 
				ps3.setLong(1, rs2.getLong("tag"));
			}
			
			rs3 = ps3.executeQuery();
			
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
				currIncome = new Income(incomeOwner, incomeAmount, incomeTags,
						new SimpleDateFormat("yyyy-MM-dd").format(nextDate.getTime()), incomeDescription, id);
				
				Income.add(currIncome);
				nextDate.add(Calendar.DATE, (int) days);
				nextDate.add(Calendar.MONTH, (int) months);
			}
			
			// update date in scheduled incomes table
			ps4 = connection.prepareStatement("UPDATE scheduled_incomes SET date_next = ? WHERE id = ?");
			ps4.setDate(1, new java.sql.Date(nextDate.getTime().getTime()));
			ps4.setLong(2, id);
			ps4.executeUpdate();
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
}