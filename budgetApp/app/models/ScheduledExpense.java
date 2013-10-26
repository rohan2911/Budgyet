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
 * ScheduledExpense
 * 
 * Creates Expense instances based on a user specified date and repetition period.
 * Invoked by the Schedule Actor on a period specified in Global
 *
 */

public class ScheduledExpense {
	public Date date_next;
	public long period;
	public long owner;
	public BigDecimal expense_amount;
	public String expense_description;
	public List<String> tags;
	
	/**
	 * Constructor
	 * 
	 * @param date_next - the date the Expense was most recently created on 
	 * @param period - the number of days between payment recurrence  
	 */
	
	public ScheduledExpense (String date_next, String period, String Expense_owner, String expense_amount, String expense_description, String tags) {
		try {
			this.date_next = new SimpleDateFormat("yyyy-MM-dd").parse(date_next);
		} catch (ParseException e) {
			this.date_next = null;
			e.printStackTrace();
		}
		this.period = Long.parseLong(period);
		this.owner = Long.parseLong(Expense_owner);
		this.expense_amount = new BigDecimal(expense_amount).setScale(2, RoundingMode.HALF_UP);
		this.expense_description = expense_description;
		this.tags = new ArrayList<String>(Arrays.asList(tags.split(",")));
	}
	
	/**
	 * adds scheduled Expense to the db
	 * 
	 * @param scheduledExpense
	 * @return scheduledExpense Id
	 */
	
	public static long add(ScheduledExpense scheduledExpense) {
		Connection connection = DB.getConnection();
		PreparedStatement ps1 = null;
		PreparedStatement ps2 = null;
		PreparedStatement ps3 = null;
		PreparedStatement ps4 = null;
		ResultSet rs = null;
		ResultSet generatedKeys = null;
		long scheudled_Expense_id = 0;
		
		
		try {
			// insert values into DB
			ps1 = connection.prepareStatement("INSERT INTO scheduled_expenses (date_next, period, owner, expense_amount, expense_description) VALUES (?, ?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
			ps1.setDate(1, new java.sql.Date(scheduledExpense.date_next.getTime()));
			ps1.setLong(2, scheduledExpense.period);
			ps1.setLong(3, scheduledExpense.owner);
			ps1.setBigDecimal(4, scheduledExpense.expense_amount);
			ps1.setString(5, scheduledExpense.expense_description);
			ps1.executeUpdate();
			
			// get the id to return
			generatedKeys = ps1.getGeneratedKeys();
			
			// process tags string (code pulled from Expense add)
			if (generatedKeys.next()) {
				scheudled_Expense_id = generatedKeys.getLong(1);
				
				// insert the tags
				ps2 = connection.prepareStatement("insert into Expenses_tags (owner, name) select * from (select ?, ?) as tmp where not exists (select 1 from Expenses_tags where owner = ? and name = ?)");
				ps2.setLong(1, scheduledExpense.owner);
				ps2.setLong(3, scheduledExpense.owner);
				Iterator<String> tags_it = scheduledExpense.tags.iterator();
				while (tags_it.hasNext()) {
					String tag = tags_it.next();
					ps2.setString(2, tag);
					ps2.setString(4, tag);
					ps2.executeUpdate();
				}
				
				// get tag ids
				String sql3 = "select id from Expenses_tags where owner = ? and (name = ?";
				for (int i=0; i<scheduledExpense.tags.size()-1; i++) {
					sql3 += " OR name = ?";
				}
				sql3 += ")";
				ps3 = connection.prepareStatement(sql3);
				ps3.setLong(1, scheduledExpense.owner);
				for (int i=0; i<scheduledExpense.tags.size(); i++) {
					ps3.setString(i+2, scheduledExpense.tags.get(i));
				}
				rs = ps3.executeQuery();
				
				// insert the Expense tag mapping
				ps4 = connection.prepareStatement("insert into scheduled_expenses_tags_map (scheduled_expense, tag) values (?, ?)");
				ps4.setLong(1, scheudled_Expense_id);
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
		
		return scheudled_Expense_id;
	}
	
	/**
	 * 
	 * Scheduled Task is run daily to ensure that scheculed Expenses are added periodically
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
			
			// we select the Expense schedulers who's next scheduled payment time has elapsed
			ps1 = connection.prepareStatement("SELECT * FROM scheduled_expenses WHERE date_next < ?");
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
		PreparedStatement ps1 = null;
		ResultSet rs = null;
		
		try {
			ps1 = connection.prepareStatement("SELECT * FROM scheduled_expenses WHERE id = ?");
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
			
			long dbPeriod = rs.getLong("period");
			Date dbDate = rs.getDate("date_next");
			
			Calendar nextDate = Calendar.getInstance();
			nextDate.setTime(dbDate);
			
			// populate strings so we can initialise a new Expense
			String ExpenseOwner = String.valueOf(rs.getLong("owner"));
			String ExpenseAmount = rs.getBigDecimal("expense_amount").toString();
			String ExpenseDescription = rs.getString("expense_description");
			
			
			ps2 = connection.prepareStatement("SELECT * FROM scheduled_expenses_tags_map WHERE scheduled_expense = ?");
			ps2.setLong(1, id);
			rs2 = ps2.executeQuery();
			
			// get all linked tags
			ps3 = connection.prepareStatement("SELECT * FROM expenses_tags WHERE id IN (?)");
			
			while (rs2.next()) { 
				ps3.setLong(1, rs2.getLong("tag"));
			}
			
			rs3 = ps3.executeQuery();
			
			String ExpenseTags = new String();
			
			// aggregate tag names in a string for the Expense constructor
			while (rs3.next()) {
				ExpenseTags = ExpenseTags.concat(rs3.getString("name"));
				ExpenseTags = ExpenseTags.concat(",");
			}
			
			// trim excess comma
			ExpenseTags = ExpenseTags.substring(0, (ExpenseTags.length() - 1));
			
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
			
			Expense currExpense = null;
			
			// add the period to the next date the scheduled transaction is to be made until we have covered all the scheduled transactions
			while (nextDate.getTime().getTime() < currentDate.getTime() ) {
				currExpense = new Expense(ExpenseOwner, ExpenseAmount, ExpenseTags,
						new SimpleDateFormat("yyyy-MM-dd").format(nextDate.getTime()), ExpenseDescription, id);
				
				Expense.add(currExpense);
				nextDate.add(Calendar.DATE, (int) days);
				nextDate.add(Calendar.MONTH, (int) months);
			}
			
			// update date in scheduled Expenses table
			ps4 = connection.prepareStatement("UPDATE scheduled_expenses SET date_next = ? WHERE id = ?");
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