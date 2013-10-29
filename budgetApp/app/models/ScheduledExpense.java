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
 * ScheduledExpense
 * 
 * Creates Expense instances based on a user specified date and repetition period.
 * Invoked by the Schedule Actor on a period specified in Global
 *
 */

public class ScheduledExpense {
	public Date date_next;
	public int period;
	public long owner;
	public BigDecimal expense_amount;
	public String expense_description;
	public String tagName;
	
	/**
	 * Constructor
	 * 
	 * @param date_next - the date the Expense was most recently created on 
	 * @param period - the number of days between payment recurrence  
	 */
	
	public ScheduledExpense (String date_next, String period, String Expense_owner, String expense_amount, String expense_description, String tag) {
		try {
			this.date_next = new SimpleDateFormat("yyyy-MM-dd").parse(date_next);
		} catch (ParseException e) {
			this.date_next = null;
			e.printStackTrace();
		}
		this.period = Integer.parseInt(period);
		this.owner = Long.parseLong(Expense_owner);
		this.expense_amount = new BigDecimal(expense_amount).setScale(2, RoundingMode.HALF_UP);
		this.expense_description = expense_description;
		this.tagName = tag;
	}
	
	/**
	 * adds scheduled Expense to the db
	 * 
	 * @param scheduledExpense
	 * @return scheduledExpense Id
	 */
	
	public static long add(ScheduledExpense scheduledExpense) {
		Connection connection = DB.getConnection();
		PreparedStatement psInsExp = null;
		PreparedStatement psInsTag = null;
		PreparedStatement psTagId = null;
		ResultSet rsTagId = null;
		ResultSet rsSchKey = null;
		long schId = -1;
		
		try {

			// insert the tag
			psInsTag = connection.prepareStatement("insert into expenses_tags (owner, name) select * from (select ?, ?) as tmp "
					+ "where not exists (select 1 from expenses_tags where owner = ? and name = ?)");
			psInsTag.setLong(1, scheduledExpense.owner);
			psInsTag.setLong(3, scheduledExpense.owner);
			psInsTag.setString(2, scheduledExpense.tagName);
			psInsTag.setString(4, scheduledExpense.tagName);
			psInsTag.executeUpdate();
			
			// get the tag's id
			psTagId = connection.prepareStatement("select id from expenses_tags where owner = ? and name = ?");
			psTagId.setLong(1, scheduledExpense.owner);
			psTagId.setString(2, scheduledExpense.tagName);
			rsTagId = psTagId.executeQuery();

			// get the tag id, insert the scheduled expense with this id
			if (rsTagId.next()) {
				long tagId = rsTagId.getLong(1);
				// insert the scheduled expense
				psInsExp = connection.prepareStatement("INSERT INTO scheduled_expenses (date_next, period, owner, "
						+ "expense_amount, expense_description, tag) VALUES (?, ?, ?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
				psInsExp.setDate(1, new java.sql.Date(scheduledExpense.date_next.getTime()));
				psInsExp.setInt(2, scheduledExpense.period);
				psInsExp.setLong(3, scheduledExpense.owner);
				psInsExp.setBigDecimal(4, scheduledExpense.expense_amount);
				psInsExp.setString(5, scheduledExpense.expense_description);
				psInsExp.setLong(6, tagId);
				psInsExp.executeUpdate();
				
				rsSchKey = psInsExp.getGeneratedKeys();
				
				// process tags string (code pulled from Expense add)
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
	 * Scheduled Task is run daily to ensure that scheduled Expenses are added periodically
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
			
			// we select the Expense schedulers who's next scheduled payment time has elapsed
			ps1 = connection.prepareStatement("SELECT * FROM scheduled_expenses WHERE date_next <= ?");
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
	 * Adds Expenses for a newly created scheduled Expense 
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
			ps = connection.prepareStatement("SELECT * FROM scheduled_expenses WHERE id = ?");
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
	 * Using a scheduled Expenses details add the Expenses due for a schedule based on current system time
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
			
			// populate strings so we can initialise a new Expense
			String expenseOwner = String.valueOf(rs.getLong("owner"));
			String expenseAmount = rs.getBigDecimal("expense_amount").toString();
			String expenseDescription = rs.getString("expense_description");
			
			
			// get all linked tags
			ps3 = connection.prepareStatement("SELECT * FROM expenses_tags WHERE id = ?");
			ps3.setLong(1, tagId);
			rs3 = ps3.executeQuery();
			if(rs3.next()) {
				String expenseTag = rs3.getString("name");
				
				
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
				
				Expense currExpense = null;
				
				// add the period to the next date the scheduled transaction is to be made until we have covered all the scheduled transactions
				while (nextDate.getTime().getTime() <= currentDate.getTime() ) {
					currExpense = new Expense(expenseOwner, expenseAmount, expenseTag,
							new SimpleDateFormat("yyyy-MM-dd").format(nextDate.getTime()), expenseDescription, id);
					
					Expense.add(currExpense);
					nextDate.add(Calendar.DATE, (int) days);
					nextDate.add(Calendar.MONTH, (int) months);
				}
				
				// update date in scheduled Expenses table
				ps4 = connection.prepareStatement("UPDATE scheduled_expenses SET date_next = ? WHERE id = ?");
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
	 * Updates a scheduled expense and, expense instances created by that scheduled expense
	 * 
	 * Expense instances are updated if they were created after the date in the scheduled expense
	 * object.
	 * 
	 * @param scheduledExpenseId
	 * @return
	 */
	public boolean update(long scheduledExpenseId) {
		boolean success = true;
		Connection connection =  DB.getConnection();
		
		PreparedStatement psGetScheduledExpense = null;
		PreparedStatement psGetAssociatedExpenses = null;
		PreparedStatement psRemoveExpenses = null;
		PreparedStatement psGetTag = null;
		PreparedStatement psCreateTag = null;
		PreparedStatement psUpdateScheduled = null;
		
		ResultSet rsGetScheduledExpense = null;
		ResultSet rsGetAssociatedExpenses = null;
		ResultSet rsGetTag = null;
		ResultSet rsTagKey = null;
		
		try {
			// get the scheduled expense record
			psGetScheduledExpense = connection.prepareStatement("SELECT * FROM scheduled_expenses WHERE id = ?");
			psGetScheduledExpense.setLong(1, scheduledExpenseId);
			rsGetScheduledExpense = psGetScheduledExpense.executeQuery();
			
			if (rsGetScheduledExpense.next()) {
				// get a list of expenses created after the date specified in ScheduledExpense
				psGetAssociatedExpenses = connection.prepareStatement("SELECT * FROM expenses WHERE scheduler = ? AND date_occur >= ?");
				psGetAssociatedExpenses.setLong(1, rsGetScheduledExpense.getLong("id"));
				psGetAssociatedExpenses.setDate(2, new java.sql.Date(this.date_next.getTime()));
				rsGetAssociatedExpenses = psGetAssociatedExpenses.executeQuery();
				
				// remove expenses from date so we can recreate them with init()
				while (rsGetAssociatedExpenses.next()) {
					psRemoveExpenses = connection.prepareStatement("DELETE FROM expenses WHERE id = ?");
					psRemoveExpenses.setLong(1, rsGetAssociatedExpenses.getLong("id"));
					psRemoveExpenses.executeUpdate();
				}
				
				if (this.period > 0) {
					// check if the tag exists and get id (or create the tag)
					psGetTag = connection.prepareStatement("SELECT * FROM expenses_tags WHERE name = ? AND owner = ?");
					psGetTag.setString(1, this.tagName);
					psGetTag.setLong(2, this.owner);
					rsGetTag = psGetTag.executeQuery();
					
					long tagId = 0;
					
					if (rsGetTag.next()) {
						tagId = rsGetTag.getLong("id");
					} else {
						psCreateTag = connection.prepareStatement("INSERT INTO expenses_tags (name, owner) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
						psCreateTag.setString(1, this.tagName);
						psCreateTag.setLong(2, this.owner);
						psCreateTag.executeUpdate();
						rsTagKey = psCreateTag.getGeneratedKeys();
						if (rsTagKey.next()) {
							tagId = rsTagKey.getLong(1);
						}
					}
					
					
					psUpdateScheduled = connection.prepareStatement("UPDATE scheduled_expenses SET date_next = ?, period = ?, "
							+ "owner = ?, expense_amount = ?, expense_description = ?, tag = ? WHERE id = ?");
					psUpdateScheduled.setDate(1, new java.sql.Date(this.date_next.getTime()));
					psUpdateScheduled.setInt(2, this.period);
					psUpdateScheduled.setLong(3, this.owner);
					psUpdateScheduled.setBigDecimal(4, this.expense_amount);
					psUpdateScheduled.setString(5, this.expense_description);
					psUpdateScheduled.setLong(6, tagId);
					
					psUpdateScheduled.setLong(7, scheduledExpenseId);
					
					psUpdateScheduled.executeUpdate();
				} else {
					// period = 0, we recreate the expense with new details
					Expense replacement = new Expense(Long.toString(this.owner), this.expense_amount.toString(), this.tagName, 
							new SimpleDateFormat("yyyy-MM-dd").format(this.date_next.getTime()), this.expense_description);
					Expense.add(replacement);
					remove(scheduledExpenseId);
				}
				
				// recreate the expense instances with the new expense details
				ScheduledExpense.init(scheduledExpenseId);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			success = false;
		} finally {
			try {
				if (rsGetScheduledExpense != null) {
					rsGetScheduledExpense.close();
				}
				if (rsGetAssociatedExpenses != null) {
					rsGetAssociatedExpenses.close();
				}
				if (rsGetTag != null) {
					rsGetTag.close();
				}
				if (rsTagKey != null) {
					rsTagKey.close();
				}
				if (psGetScheduledExpense != null) {
					psGetScheduledExpense.close();
				}
				if (psGetAssociatedExpenses != null) {
					psGetAssociatedExpenses.close();
				}
				if (psRemoveExpenses != null) {
					psRemoveExpenses.close();
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
	 * Gets the scheduler for a given expense
	 * 
	 * @param the id of the expense
	 * @return a Scheduled expense populated with values from the database
	 */
	public static ScheduledExpense get(long expenseId) {
		ScheduledExpense returnScheduler = null; 
		Connection connection = DB.getConnection();
		
		PreparedStatement psExpenseSelect = null;
		PreparedStatement psSchedulerSelect = null;
		ResultSet rsExpense = null;
		ResultSet rsScheduler = null;
		
		try {
			// select the scheduler field of the record for the expense id
			psExpenseSelect = connection.prepareStatement("SELECT scheduler FROM expenses WHERE id = ?");
			psExpenseSelect.setLong(1, expenseId);
			rsExpense = psExpenseSelect.executeQuery();
			
			if (rsExpense.next()) {
				long schedulerId = rsExpense.getLong(1);
				
				// select the schedueler object
				psSchedulerSelect = connection.prepareStatement("SELECT * FROM scheduled_expenses WHERE id = ?");
				psSchedulerSelect.setLong(1, schedulerId);
				rsScheduler = psSchedulerSelect.executeQuery();
				if (rsScheduler.next()) {
					// set return value
					returnScheduler = new ScheduledExpense(rsScheduler.getString("date_next"), rsScheduler.getString("period"),
							rsScheduler.getString("owner"), rsScheduler.getString("expense_amount"),
							rsScheduler.getString("expense_description"), rsScheduler.getString("tag"));
				}
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (rsExpense != null) {
					rsExpense.close();
				}
				
				if (rsScheduler != null) {
					rsScheduler.close();
				}
				
				if (psExpenseSelect != null) {
					psExpenseSelect.close();
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
	 * removes the expense scheduler at id from the database
	 * 
	 * @returns success
	 */
	
	public static boolean remove(long id) {
		boolean success = true;
		
		Connection connection = DB.getConnection();
		PreparedStatement psUpdateExpenses = null;
		PreparedStatement psScheduledExpenseDelete = null;
		
		try {
			psUpdateExpenses = connection.prepareStatement("UPDATE expenses SET scheduler = NULL WHERE scheduler = ?");
			psUpdateExpenses.setLong(1, id);
			psUpdateExpenses.executeUpdate();
			
			psScheduledExpenseDelete = connection.prepareStatement("DELETE FROM scheduled_expenses WHERE id = ?");
			psScheduledExpenseDelete.setLong(1, id);
			psScheduledExpenseDelete.executeUpdate();
			
			
		} catch (SQLException e) {
			success = false;
			e.printStackTrace();
		} finally {
			try {
				if (psUpdateExpenses != null) {
					psUpdateExpenses.close();
				}
				
				if (psScheduledExpenseDelete != null) {
					psScheduledExpenseDelete.close();
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
	 * Removes a scheduler if there are no expenses tied to it
	 * 
	 * @param id - id of the scheduler being removed
	 * @return success
	 */
	public static boolean clean(long id) {
		boolean success = true;
		Connection connection = DB.getConnection();
		
		
		PreparedStatement psRemoveScheduler = null; 
		
		try {
			psRemoveScheduler = connection.prepareStatement("DELETE FROM scheduled_expenses WHERE scheduled_expenses.id = ? AND "
					+ "NOT EXISTS (SELECT * FROM expenses WHERE expenses.scheduler = ?)");
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