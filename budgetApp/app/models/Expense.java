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
	public String tagName;
	public Date date_occur;
	public String date_display;
	public String description;
	public Long scheduler;
	public int period;	// time period of the schedule
	
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
		this.date_display = date_occur;
		try {
			// simpledateformat is a JAVA date. 
			this.date_occur = new SimpleDateFormat("yyyy-MM-dd").parse(date_occur);
		} catch (ParseException e) {
			this.date_occur = null;
			e.printStackTrace();
		}
		this.description = description;
		this.scheduler = scheduler; 
		this.period = 0;
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
		this.date_display = date;
		try {
			// simpledateformat is a JAVA date. 
			this.date_occur = new SimpleDateFormat("yyyy-MM-dd").parse(date);
		} catch (ParseException e) {
			this.date_occur = null;
			e.printStackTrace();
		}
		this.description = desc;
		this.scheduler = (Long) null;
		this.period = 0;
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
	 * Get all the expenses belonging to specified user
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
				expenses.add(i);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return expenses;
	}
	
	/**
	 * Gets the list of the owner's current income tags. 
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
	
	/**
	 * @param accId user account id as stored in the db. 
	 * @return the list of summed values of the tag names(owned by the user) found by getTags().
	 */
	public static List<String> getTagSum(String accId) {
		List<String> tagSums = new ArrayList<String>();
		List<String> tagNameList = getTags(accId);
		
		System.out.println(tagNameList);
		Connection connection = DB.getConnection();
		for (String tag: tagNameList) {
			System.out.println("current tag:"+tag);
			try {
				PreparedStatement ps = connection.prepareStatement("select sum(amount) as total from expenses i join expenses_tags it "
						+ "on i.tag = it.id and it.owner = ? where it.name = ?");
				ps.setLong(1, Long.parseLong(accId));
				ps.setString(2, tag);
				ResultSet rs = ps.executeQuery();
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
		return tagSums;
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
		tags = tags.substring(0, tags.length()-1);
		return tags;
	}
	
}