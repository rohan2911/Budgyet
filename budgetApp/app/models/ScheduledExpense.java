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
 * ScheduledExpense
 * 
 * Creates Expense instances based on a user specified date and repetition period.
 * Invoked by the Schedule Actor on a period specified in Global
 *
 */

public class ScheduledExpense {
	public Date date_next;
	public long period;
	
	/**
	 * Constructor
	 * 
	 * @param date_last - the date the Expense was most recently created on 
	 * @param period - the number of days between payment reccurrences  
	 */
	
	public ScheduledExpense (String date_last, String period) {
		try {
			this.date_next = new SimpleDateFormat("yyyy-MM-dd").parse(date_last);
		} catch (ParseException e) {
			this.date_next = null;
			e.printStackTrace();
		}
		
		this.period = Long.getLong(period);
	}
	
	
	
	public static boolean add(ScheduledExpense scheduledExpense) {
		
		return false;
	}
	
	/**
	 * 
	 * Scheduled Task is run daily to ensure that scheculed Expenses are added periodically
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
			
			// we select the Expense schedulers who's next scheduled payment time has elapsed
			ps1 = connection.prepareStatement("SELECT * FROM expenses_scheduled WHERE date_next < ?");
			ps1.setDate(1, new java.sql.Date(currentDate.getTime()));
			rs = ps1.executeQuery();
			
			while (!rs.isAfterLast()) {
				long id = rs.getLong("id");
				
				long dbPeriod = rs.getLong("period");
				Date dbDate = rs.getDate("date_next");
				
				Calendar nextDate = Calendar.getInstance();
				nextDate.setTime(dbDate);
				
				// fetch the details of an Expense associated with the scheduler  
				ps2 = connection.prepareStatement("SELECT * FROM expenses WHERE scheduler = ?");
				ps2.setLong(1, id);
				rs2 = ps2.executeQuery();
				
				// populate strings with result so we can initialise a new Expense
				String ExpenseOwner = String.valueOf(rs2.getLong("owner"));
				String ExpenseAmount = rs2.getBigDecimal("amount").toString();
				String ExpenseDescription = rs2.getString("description");
				
				// get links from tag - Expense link table
				long ExpenseId = rs2.getLong("id");
				String tags = new String();
				
				ps3 = connection.prepareStatement("SELECT * FROM Expenses_tags_map WHERE Expense = ?");
				ps3.setLong(1, ExpenseId);
				rs3 = ps3.executeQuery();
				
				// get all linked tags
				ps4 = connection.prepareStatement("SELECT * FROM Expense_tags WHERE id IN (?)");
				
				while (!rs3.isAfterLast()) { 
					ps4.setLong(1, rs3.getLong("tag"));
					rs3.next();
				}
				
				rs3 = ps4.executeQuery();
				
				String ExpenseTags = new String();
				
				// aggregate tag names in a string for the Expense constructor
				while (!rs3.isAfterLast()) {
					ExpenseTags = ExpenseTags.concat(rs3.getString("name"));
					ExpenseTags = ExpenseTags.concat(",");
					rs3.next();
				}
				
				// trim excess comma
				ExpenseTags = ExpenseTags.substring(0, (ExpenseTags.length() - 1));
				
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
				
				Expense currExpense = null;
				
				// add the period to the next date the scheduled transaction is to be made until we have covered all the scheduled transactions
				while (nextDate.getTime().getTime() < currentDate.getTime() ) {
					
					currExpense = new Expense(ExpenseOwner, ExpenseAmount, ExpenseTags,
							new SimpleDateFormat("yyyy-MM-dd").format(nextDate), ExpenseDescription);
					
					Expense.add(currExpense);
					
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