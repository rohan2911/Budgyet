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
	
	public long owner;
	public BigDecimal amount;
	public String tagName;
	public Date date_occur;
	public String description;
	public Long scheduler;
	
	/**
	 * Constructor for repeating expenses.
	 * @param owner id of the owner (currently logged in user id)
	 * @param amount user specified amount of this expense
	 * @param tag the user specified tag name associated with this expense
	 * @param date_occur user specified date of this expense
	 * @param description user specified description of this expense
	 * @param scheduler id of the schedular to be used for this repeating expense
	 */
	public Expense(String owner, String amount, String tag, String date_occur, String description, long scheduler) {
		this.owner = Long.parseLong(owner);
		this.amount = new BigDecimal(amount).setScale(2, RoundingMode.HALF_UP);
		this.tagName = tag;
		try {
			// simpledateformat is a JAVA date. 
			this.date_occur = new SimpleDateFormat("yyyy-MM-dd").parse(date_occur);
		} catch (ParseException e) {
			this.date_occur = null;
			e.printStackTrace();
		}
		this.description = description;
		this.scheduler = scheduler; 
	}
	
	/**
	 * Overloaded constructor for for non-repeating expenses.
	 * @param owner owner id of the owner (currently logged in user id)
	 * @param amount user specified amount of this expense
	 * @param tag the tag associated with this expense
	 * @param date user specified date of this expense
	 * @param desc user specified description of this expense
	 */
	public Expense(String owner, String amount, String tag, String date, String desc) {
		this.owner = Long.parseLong(owner);
		this.amount = new BigDecimal(amount).setScale(2, RoundingMode.HALF_UP);
		this.tagName = tag;
		try {
			// simpledateformat is a JAVA date. 
			this.date_occur = new SimpleDateFormat("yyyy-MM-dd").parse(date);
		} catch (ParseException e) {
			this.date_occur = null;
			e.printStackTrace();
		}
		this.description = desc;
		this.scheduler = (Long) null;
	}

	public static boolean add(Expense expense) {
		
		Connection connection = DB.getConnection();
		PreparedStatement psInsExp = null;
		PreparedStatement psInsTag = null;
		PreparedStatement psTagId = null;
		ResultSet rsTagId = null;
		
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
				// insert the expense
				psInsExp = connection.prepareStatement("insert into expenses (owner, amount, description, date_occur, scheduler, tag) "
						+ "values (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
				psInsExp.setLong(1, expense.owner);
				psInsExp.setBigDecimal(2, expense.amount);
				psInsExp.setString(3, expense.description);
				psInsExp.setDate(4, new java.sql.Date(expense.date_occur.getTime()));
				if (expense.scheduler != null) {
					psInsExp.setLong(5, expense.scheduler);
				} else {
					psInsExp.setNull(5, java.sql.Types.BIGINT);
				}
				psInsExp.setLong(6, tagId);
				psInsExp.executeUpdate();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
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
	
	public static List<String> getTags(String accId) {
		List<String> tagList = new ArrayList<String>();
		
		Connection connection = DB.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			ps = connection.prepareStatement("SELECT name FROM expenses_tags WHERE owner = ?");
			ps.setLong(1, Long.parseLong(accId));
			rs = ps.executeQuery();
			
			while (rs.next()) {
				tagList.add(rs.getString("name"));
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return tagList;
	}
	
}