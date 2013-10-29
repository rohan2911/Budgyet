package models;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
	 * Scheduled Task is run daily to ensure that scheduled Incomes are added periodically
	 * on a user specified period given on creation.
	 * 
	 * @return success
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
			ps1 = connection.prepareStatement("SELECT * FROM scheduled_incomes WHERE date_next <= ?");
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
					return false;
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
	 * Updates a scheduled income and, income instances created by that scheduled income
	 * 
	 * Income instances are updated if they were created after the date in the schedueled income
	 * object.
	 * 
	 * @param scheduledIncomeId
	 * @return
	 */
	public boolean update(long scheduledIncomeId) {
		boolean success = true;
		Connection connection =  DB.getConnection();
		
		PreparedStatement psGetScheduledIncome = null;
		PreparedStatement psGetAssociatedIncomes = null;
		PreparedStatement psRemoveIncomes = null;
		PreparedStatement psGetTag = null;
		PreparedStatement psCreateTag = null;
		PreparedStatement psUpdateScheduled = null;
		
		ResultSet rsGetScheduledIncome = null;
		ResultSet rsGetAssociatedIncomes = null;
		ResultSet rsGetTag = null;
		ResultSet rsTagKey = null;
		
		try {
			// get the scheduled income record
			psGetScheduledIncome = connection.prepareStatement("SELECT * FROM scheduled_incomes WHERE id = ?");
			psGetScheduledIncome.setLong(1, scheduledIncomeId);
			rsGetScheduledIncome = psGetScheduledIncome.executeQuery();
			
			if (rsGetScheduledIncome.next()) {
				// get a list of incomes created after the date specified in ScheduledIncome
				psGetAssociatedIncomes = connection.prepareStatement("SELECT * FROM incomes WHERE scheduler = ? AND date_occur >= ?");
				psGetAssociatedIncomes.setLong(1, rsGetScheduledIncome.getLong("id"));
				psGetAssociatedIncomes.setDate(2, new java.sql.Date(this.date_next.getTime()));
				rsGetAssociatedIncomes = psGetAssociatedIncomes.executeQuery();
				
				// remove incomes from date so we can recreate them with init()
				while (rsGetAssociatedIncomes.next()) {
					psRemoveIncomes = connection.prepareStatement("DELETE FROM incomes WHERE id = ?");
					psRemoveIncomes.setLong(1, rsGetAssociatedIncomes.getLong("id"));
					psRemoveIncomes.executeUpdate();
				}
				
				if (this.period > 0) {
					// check if the tag exists and get id (or create the tag)
					psGetTag = connection.prepareStatement("SELECT * FROM incomes_tags WHERE name = ? AND owner = ?");
					psGetTag.setString(1, this.tagName);
					psGetTag.setLong(2, this.owner);
					rsGetTag = psGetTag.executeQuery();
					
					long tagId = 0;
					
					if (rsGetTag.next()) {
						tagId = rsGetTag.getLong("id");
					} else {
						psCreateTag = connection.prepareStatement("INSERT INTO incomes_tags (name, owner) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
						psCreateTag.setString(1, this.tagName);
						psCreateTag.setLong(2, this.owner);
						psCreateTag.executeUpdate();
						rsTagKey = psCreateTag.getGeneratedKeys();
						if (rsTagKey.next()) {
							tagId = rsTagKey.getLong(1);
						}
					}
					
					
					psUpdateScheduled = connection.prepareStatement("UPDATE scheduled_incomes SET date_next = ?, period = ?, "
							+ "owner = ?, income_amount = ?, income_description = ?, tag = ? WHERE id = ?");
					psUpdateScheduled.setDate(1, new java.sql.Date(this.date_next.getTime()));
					psUpdateScheduled.setInt(2, this.period);
					psUpdateScheduled.setLong(3, this.owner);
					psUpdateScheduled.setBigDecimal(4, this.income_amount);
					psUpdateScheduled.setString(5, this.income_description);
					psUpdateScheduled.setLong(6, tagId);
					
					psUpdateScheduled.setLong(7, scheduledIncomeId);
					
					psUpdateScheduled.executeUpdate();
				} else {
					// period = 0, we recreate the income with new details
					Income replacement = new Income(Long.toString(this.owner), this.income_amount.toString(), this.tagName, 
							new SimpleDateFormat("yyyy-MM-dd").format(this.date_next.getTime()), this.income_description);
					Income.add(replacement);
					remove(scheduledIncomeId);
				}
				
				// recreate the income instances with the new income details
				ScheduledIncome.init(scheduledIncomeId);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			success = false;
		} finally {
			try {
				if (rsGetScheduledIncome != null) {
					rsGetScheduledIncome.close();
				}
				if (rsGetAssociatedIncomes != null) {
					rsGetAssociatedIncomes.close();
				}
				if (rsGetTag != null) {
					rsGetTag.close();
				}
				if (rsTagKey != null) {
					rsTagKey.close();
				}
				if (psGetScheduledIncome != null) {
					psGetScheduledIncome.close();
				}
				if (psGetAssociatedIncomes != null) {
					psGetAssociatedIncomes.close();
				}
				if (psRemoveIncomes != null) {
					psRemoveIncomes.close();
				}
				if (psGetTag != null) {
					psGetTag.close();
				}
				if (psCreateTag != null) {
					psCreateTag.close();
				}
				if (psUpdateScheduled != null) {
					psUpdateScheduled.close();
				}
				if (connection != null) {
					connection.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		return success;
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
			
			if (rsIncome.next()) {
				long schedulerId = rsIncome.getLong(1);
				
				// select the schedueler object
				psSchedulerSelect = connection.prepareStatement("SELECT * FROM scheduled_incomes WHERE id = ?");
				psSchedulerSelect.setLong(1, schedulerId);
				rsScheduler = psSchedulerSelect.executeQuery();
				if (rsScheduler.next()) {
					// set return value
					returnScheduler = new ScheduledIncome(rsScheduler.getString("date_next"), rsScheduler.getString("period"),
							rsScheduler.getString("owner"), rsScheduler.getString("income_amount"),
							rsScheduler.getString("income_description"), rsScheduler.getString("tag"));
				}
			}
			
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
	
	/**
	 * removes the income scheduler at id from the database
	 * 
	 * @returns success
	 */
	
	public static boolean remove(long id) {
		boolean success = true;
		
		Connection connection = DB.getConnection();
		PreparedStatement psUpdateIncomes = null;
		PreparedStatement psScheduledIncomeDelete = null;
		
		try {
			psUpdateIncomes = connection.prepareStatement("UPDATE incomes SET scheduler = NULL WHERE scheduler = ?");
			psUpdateIncomes.setLong(1, id);
			psUpdateIncomes.executeUpdate();
			
			psScheduledIncomeDelete = connection.prepareStatement("DELETE FROM scheduled_incomes WHERE id = ?");
			psScheduledIncomeDelete.setLong(1, id);
			psScheduledIncomeDelete.executeUpdate();
			
			
		} catch (SQLException e) {
			success = false;
			e.printStackTrace();
		} finally {
			try {
				if (psUpdateIncomes != null) {
					psUpdateIncomes.close();
				}
				
				if (psScheduledIncomeDelete != null) {
					psScheduledIncomeDelete.close();
				}
				
				if (connection != null) {
					connection.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return success;
	}
	
	/**
	 * 
	 * Removes a scheduler if there are no incomes tied to it
	 * 
	 * @param id - id of the scheduler being removed
	 * @return success
	 */
	public static boolean clean(long id) {
		boolean success = true;
		Connection connection = DB.getConnection();
		
		
		PreparedStatement psRemoveScheduler = null; 
		
		try {
			psRemoveScheduler = connection.prepareStatement("DELETE FROM scheduled_incomes WHERE scheduled_incomes.id = ? AND "
					+ "NOT EXISTS (SELECT * FROM incomes WHERE incomes.scheduler = ?)");
			psRemoveScheduler.setLong(1, id);
			psRemoveScheduler.setLong(2, id);
			psRemoveScheduler.executeUpdate();
		} catch (SQLException e) {
			success = false;
			e.printStackTrace();
		} finally {
			try {
				if (psRemoveScheduler != null) {
					psRemoveScheduler.close();
				}
				
				if (connection != null) {
					connection.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return success;
	}
}