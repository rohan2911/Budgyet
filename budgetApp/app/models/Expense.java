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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import play.db.DB;

public class Expense {

	public long id;
	public long owner;
	public BigDecimal amount;
//	public List<String> tags;
	public String tagName;	// max. 1 tag per expense 
	public Date date_occur;
	public String date_display;
	public String description;
	public Long scheduler;	// id of the scheduler assigned to the expense
	
	/**
	 * Contructor for Expense class.
	 * @param owner id of the owner of this expense. (the current logged in user's id is passed in)
	 * @param amount the amount of expense
	 * @param tagName the user specified tag name associated with this expense. (only 1 allowed)
	 * @param date_occur user specified date of the expense
	 * @param description expense description
	 * @param scheduler id of the scheduler used for repeating expenses
	 */
	public Expense(String owner, String amount, String tagName, String date_occur, String description, long scheduler) {
		this.owner = Long.parseLong(owner);
		this.amount = new BigDecimal(amount).setScale(2, RoundingMode.HALF_UP);
//		this.tags = new ArrayList<String>(Arrays.asList(tags.split(",")));
		this.tagName = tagName;
		this.date_display = date_occur;
		try {
			this.date_occur = new SimpleDateFormat("yyyy-MM-dd").parse(date_occur);
		} catch (ParseException e) {
			this.date_occur = null;
			e.printStackTrace();
		}
		this.description = description;
		this.scheduler = scheduler;
	}
	
	/**
	 * Overloading constructor, used for non-repeating expenses
	 * @param owner id of the owner of this expense. (the current logged in user's id is passed in)
	 * @param amount the amount of expense
	 * @param tagName the tag associated with this expense. (only 1 allowed)
	 * @param date user specified date of the expense
	 * @param desc expense description
	 */
	public Expense(String owner, String amount, String tagName, String date,	String desc) {
		this.owner = Long.parseLong(owner);
		this.amount = new BigDecimal(amount).setScale(2, RoundingMode.HALF_UP);
		this.tagName = tagName;
		this.date_display = date;
		try {
			this.date_occur = new SimpleDateFormat("yyyy-MM-dd").parse(date);
		} catch (ParseException e) {
			this.date_occur = null;
			e.printStackTrace();
		}
		this.description = desc;
		this.scheduler = (long) 0;
	}

	/**
	 * Adds the specified expense to the db.
	 * @param expense the Expense object that holds data to be added to the db.
	 * @return
	 */
	public static boolean add(Expense expense) {
		
		Connection connection = DB.getConnection();
		PreparedStatement psInsExpense = null;	// used to insert an expense to table expense
		PreparedStatement psInsTag = null;	// used for inserting tag into table expenses_tags
		PreparedStatement psTagId = null;	// used to get tag id
		ResultSet rsTagId = null;	// used to store tag id after fetching tag
		
		try {
			
			// insert the tag
			psInsTag = connection.prepareStatement("insert into expenses_tags (owner, name) select * from (select ?, ?) as tmp "
					+ "where not exists (select 1 from expenses_tags where owner = ? and name = ?)", Statement.RETURN_GENERATED_KEYS);
			psInsTag.setLong(1, expense.owner);
			psInsTag.setLong(3, expense.owner);
			psInsTag.setString(2, expense.tagName);
			psInsTag.setString(4, expense.tagName);
			psInsTag.executeUpdate();
			
			// get the tag's id
			String sqlTagId = "select id from expenses_tags where owner = ? and name = ?";
			psTagId = connection.prepareStatement(sqlTagId);
			psTagId.setLong(1, expense.owner);
			psTagId.setString(2, expense.tagName);
			rsTagId = psTagId.executeQuery();
			
			
			// get the tag id, insert the expense with this id
			long tagId;
			if (rsTagId.next()) {
				tagId = rsTagId.getLong(1);
				psInsExpense = connection.prepareStatement("insert into expenses (owner, amount, description, date_occur, scheduler, tag) "
						+ "values (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
				psInsExpense.setLong(1, expense.owner);
				psInsExpense.setBigDecimal(2, expense.amount);
				psInsExpense.setString(3, expense.description);
				psInsExpense.setDate(4, new java.sql.Date(expense.date_occur.getTime()));
				if (expense.scheduler != 0) {
					psInsExpense.setLong(5, expense.scheduler);
				} else {
					psInsExpense.setNull(5, java.sql.Types.BIGINT);
				}
				
				psInsExpense.setLong(6, tagId);
				psInsExpense.executeUpdate();
			} else {
				// this should never happen
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (rsTagId != null) {
					rsTagId.close();
				}
				if (psInsExpense != null) {
					psInsExpense.close();
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
		
		return true;
	}
	
	/**
	 * 
	 * updates the database record with the id
	 * using the values from the expense object
	 * 
	 * @param expenseId - Id of the expense in the database
	 * @return success
	 */
	public boolean update(long expenseId) {
		boolean success =  true;
		
		Connection connection = DB.getConnection();
		
		PreparedStatement psSelectTags = null;
		PreparedStatement psInsertTags = null;
		PreparedStatement psUpdateExpense = null;
		
		ResultSet rsSelectTags = null;
		ResultSet rsTagKey = null;
		
		try {
			// insert any new tag if it does not exist
			psSelectTags = connection.prepareStatement("SELECT * FROM expenses_tags WHERE name = ?");
			psSelectTags.setString(1, this.tagName);
			rsSelectTags = psSelectTags.executeQuery();
			
			long tagId = 0;
			
			// if the tag does not exist create it
			if (rsSelectTags.next()) {
				// get existing tag
				tagId = rsSelectTags.getLong(1);
			} else {
				// create a n
				psInsertTags = connection.prepareStatement("INSERT INTO expenses_tags (owner, name) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
				psInsertTags.setLong(1, this.owner);
				psInsertTags.setString(2, this.tagName);
				psInsertTags.executeUpdate();
				rsTagKey = psInsertTags.getGeneratedKeys();
				tagId = rsTagKey.getLong(1);
			}
			
			// update the expense database record
			psUpdateExpense = connection.prepareStatement("UPDATE expenses SET amount = ?, description = ?, date_occur = ?, tag = ? WHERE id = ?");
			psUpdateExpense.setBigDecimal(1, this.amount);
			psUpdateExpense.setString(2, this.description);
			psUpdateExpense.setDate(3, new java.sql.Date(this.date_occur.getTime()));
			psUpdateExpense.setLong(4, tagId);
			psUpdateExpense.setLong(5, tagId);
			psUpdateExpense.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			success = false;
		} finally {
			try {
				if (psSelectTags != null) {
					psSelectTags.close();
				}
				
				if (psInsertTags != null) {
					psInsertTags.close();
				}
				
				if (psUpdateExpense != null) {
					psUpdateExpense.close();
				}
				
				if (rsSelectTags != null) {
					rsSelectTags.close();
				}
				
				if (rsTagKey != null) {
					rsTagKey.close();
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
	 * Get all the expenses belonging to specified user.
	 * Used in displaying the list of expenses for the user (not the pie chart)
	 * @param accId id number of the account from db
	 * @return list of Expenses owned by the specified user.
	 */
	public static List<Expense> getExpenses(String accId) {
		List<Expense> expenses = new ArrayList<Expense>();
		// select all expenses by user
		Connection connection = DB.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			ps = connection.prepareStatement("select i.id, i.amount, i.description, i.date_occur, i.scheduler, it.name from expenses i"
					+ "	join expenses_tags it on i.tag = it.id where i.owner = ? order by i.date_occur desc;");
			ps.setLong(1, Long.parseLong(accId));
			rs = ps.executeQuery();
			
			while (rs.next()) {
				Expense i = new Expense(accId, rs.getBigDecimal("amount").toString(), rs.getString("name"), 
						rs.getDate("date_occur").toString(), rs.getString("description"), (Long) rs.getLong("scheduler"));
				i.id = rs.getLong("id");
//				System.out.println("created:"+i.date_occur);
				expenses.add(i);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return expenses;
	}
	
	/**
	 * @param accId user account id as stored in the db. 
	 * @return the list of summed values of the tag names(owned by the user) found by getTags().
	 */
	public static List<String> getTagSum(String accId) {
		List<String> tagSums = new ArrayList<String>();
		List<String> tagNameList = getTags(accId);
		Connection connection = DB.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		for (String tag: tagNameList) {
			try {
				ps  = connection.prepareStatement("select sum(amount) as total from expenses i join expenses_tags it "
						+ "on i.tag = it.id and it.owner = ? where it.name = ?");
				ps.setLong(1, Long.parseLong(accId));
				ps.setString(2, tag);
				rs = ps.executeQuery();
				BigDecimal amt = new BigDecimal("0.00");
				String sum = "";
				if (rs.next()) {
					if (rs.getBigDecimal(1) != null) {
						amt = rs.getBigDecimal(1);
					}
					sum = amt.toString();
				}
				tagSums.add(sum);
				
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
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
		return tagSums;
	}
	
	
	/**
	 * Gets the list of the owner's current expense tags, but only those with existing expenses.
	 * Used for displaying the tags on the tag cost breakdown.
	 * @param accId user account's id
	 * @return list of all the tag names that the user owns
	 */
	public static List<String> getExpenseTags(String accId) {
		List<String> tagList = new ArrayList<String>();
		
		Connection connection = DB.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			ps = connection.prepareStatement("SELECT name FROM expenses_tags it "
					+ "WHERE it.owner = ? AND EXISTS (SELECT expenses.id FROM expenses WHERE tag = it.id)");
			ps.setLong(1, Long.parseLong(accId));
			rs = ps.executeQuery();
			
			while (rs.next()) {
				tagList.add(rs.getString(1));
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
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
		return tagList;
	}
	
	/**
	 * Gets the list of the owner's current expense tags. 
	 * Used for displaying the tags on home page.
	 * @param accId user account's id
	 * @return list of all the tag names that the user owns
	 */
	public static List<String> getTags(String accId) {
		List<String> tagList = new ArrayList<String>();
		
		Connection connection = DB.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			ps = connection.prepareStatement("SELECT name FROM expenses_tags it WHERE it.owner = ?");
			ps.setLong(1, Long.parseLong(accId));
			rs = ps.executeQuery();
			
			while (rs.next()) {
				tagList.add(rs.getString(1));
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
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
		return tagList;
	}
	
	
	/**
	 * Given a list, turns the content into a string, with elements separated
	 * by commas.
	 * @param taglist
	 * @return the string containing the elements, separated by commas
	 */
	public static String listToString(List<String> taglist) {
		String tags = "";
		for (String t: taglist) {
			tags += t+",";
		}
		if (tags.length() > 0) {
			tags = tags.substring(0, tags.length()-1);
		}
		return tags;
	}
	
	/**
	 * 
	 * Get the expense with id expenseId from the database 
	 * 
	 * @param expenseId - the id of the expense you wish to get
	 * @return Expense populated with the expense record
	 */
	public static Expense get(long expenseId) {
		Expense returnExpense = null;
		
		Connection connection = DB.getConnection();
		
		PreparedStatement psExpenseSelect = null;
		PreparedStatement psTagSelect = null;
		
		ResultSet rsExpenseSelect = null;
		ResultSet rsTagSelect = null;
		
		try {
			// select the expense record
			psExpenseSelect = connection.prepareStatement("SELECT * FROM expenses WHERE id = ?");
			psExpenseSelect.setLong(1, expenseId);
			rsExpenseSelect = psExpenseSelect.executeQuery();
			
			if (rsExpenseSelect.next()) {
				
				// select the tag the expense is attatched to
				psTagSelect = connection.prepareStatement("SELECT * FROM expenses_tags WHERE id = ?");
				psTagSelect.setLong(1, rsExpenseSelect.getLong("tag"));
				rsTagSelect = psTagSelect.executeQuery();
				
				if (rsTagSelect.next()) {
					// choose the constructor based on if the expense is tied to a scheduler 
					rsExpenseSelect.getLong("scheduler");
					if (rsExpenseSelect.wasNull()) {
						returnExpense = new Expense(rsExpenseSelect.getString("owner"), rsExpenseSelect.getString("amount"),
								rsTagSelect.getString("name"), rsExpenseSelect.getString("date_occur"),
								rsExpenseSelect.getString("description"));
					} else {
						returnExpense = new Expense(rsExpenseSelect.getString("owner"), rsExpenseSelect.getString("amount"),
								rsTagSelect.getString("name"), rsExpenseSelect.getString("date_occur"),
								rsExpenseSelect.getString("description"), rsExpenseSelect.getLong("scheduler"));
					}
				}
				returnExpense.id = expenseId;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (rsExpenseSelect != null) {
					rsExpenseSelect.close();
				}
				
				if (rsTagSelect != null) {
					rsTagSelect.close();
				}
				
				if (psExpenseSelect != null) {
					psExpenseSelect.close();
				}
				
				if (psTagSelect != null) {
					psTagSelect.close();
				}
				
				if (connection != null) {
					connection.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		return returnExpense;
	}
	
	/**
	 * 
	 * Checks whether the scheduler for this expense exists based on the expense id
	 * and sets this.scheduler
	 * 
	 * 
	 * @return scheduler exists
	 */
	
	public boolean getScheduler() {
		boolean schedulerExists = false;
		
		Connection connection = DB.getConnection();
		
		PreparedStatement psSchedulerSelect = null;
		ResultSet rsSchedulerSelect = null;
		
		try {
			psSchedulerSelect = connection.prepareStatement("SELECT scheduler FROM expenses WHERE id = ?");
			psSchedulerSelect.setLong(1, this.id);
			rsSchedulerSelect = psSchedulerSelect.executeQuery();
			
			if (rsSchedulerSelect.next()) {
				long schedulerId = rsSchedulerSelect.getLong(1);
				if (!rsSchedulerSelect.wasNull()) {
					schedulerExists = true;
					this.scheduler = schedulerId;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (rsSchedulerSelect != null) {
					rsSchedulerSelect.close();
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
		return schedulerExists;
	}
	
	/**
	 * removes the expense at id from the database
	 * 
	 * @returns success
	 */
	
	public static boolean remove(long id) {
		boolean success = true;
		
		Connection connection = DB.getConnection();
		
		PreparedStatement psExpenseDelete = null;
		
		try {
			psExpenseDelete = connection.prepareStatement("DELETE FROM expenses WHERE id = ?");
			psExpenseDelete.setLong(1, id);
			psExpenseDelete.executeUpdate();
		} catch (SQLException e) {
			success = false;
			e.printStackTrace();
		} finally {
			try {
				if (psExpenseDelete != null) {
					psExpenseDelete.close();
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
