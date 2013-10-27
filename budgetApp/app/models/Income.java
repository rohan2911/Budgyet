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

public class Income {

	public long owner;
	public BigDecimal amount;
//	public List<String> tags;
	public String tagName;	// max. 1 tag per income 
	public Date date_occur;
	public String description;
	public Long scheduler;
	
	/**
	 * Contructor for Income class.
	 * @param owner id of the owner of this income. (the current logged in user's id is passed in)
	 * @param amount the amount of income
	 * @param tagName the tag associated with this income. (only 1 allowed)
	 * @param date_occur user specified date of the income
	 * @param description income description
	 * @param scheduler id of the scheduler used for repeating incomes
	 */
	public Income(String owner, String amount, String tagName, String date_occur, String description, long scheduler) {
		this.owner = Long.parseLong(owner);
		this.amount = new BigDecimal(amount).setScale(2, RoundingMode.HALF_UP);
//		this.tags = new ArrayList<String>(Arrays.asList(tags.split(",")));
		this.tagName = tagName;
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
	 * Overloading constructor, used for non-repeating incomes
	 * @param owner id of the owner of this income. (the current logged in user's id is passed in)
	 * @param amount the amount of income
	 * @param tagName the tag associated with this income. (only 1 allowed)
	 * @param date user specified date of the income
	 * @param desc income description
	 */
	public Income(String owner, String amount, String tagName, String date,	String desc) {
		this.owner = Long.parseLong(owner);
		this.amount = new BigDecimal(amount).setScale(2, RoundingMode.HALF_UP);
		this.tagName = tagName;
		try {
			this.date_occur = new SimpleDateFormat("yyyy-MM-dd").parse(date);
		} catch (ParseException e) {
			this.date_occur = null;
			e.printStackTrace();
		}
		this.description = desc;
		this.scheduler = (Long) null;
	}

	/**
	 * Adds the specified income to the db.
	 * @param income the Income object that holds data to be added to the db.
	 * @return
	 */
	public static boolean add(Income income) {
		
		Connection connection = DB.getConnection();
		PreparedStatement psInsIncome = null;	// used to insert an income to table income
		PreparedStatement psInsTag = null;	// used for inserting tag into table incomes_tags
		PreparedStatement psTagId = null;	// used to get tag id
		ResultSet rsTagId = null;	// used to store tag id after fetching tag
		
		try {
			
			// insert the tag
			psInsTag = connection.prepareStatement("insert into incomes_tags (owner, name) select * from (select ?, ?) as tmp "
					+ "where not exists (select 1 from incomes_tags where owner = ? and name = ?)", Statement.RETURN_GENERATED_KEYS);
			psInsTag.setLong(1, income.owner);
			psInsTag.setLong(3, income.owner);
			psInsTag.setString(2, income.tagName);
			psInsTag.setString(4, income.tagName);
			psInsTag.executeUpdate();
			
			// get the tag's id
			String sqlTagId = "select id from incomes_tags where owner = ? and name = ?";
			psTagId = connection.prepareStatement(sqlTagId);
			psTagId.setLong(1, income.owner);
			psTagId.setString(2, income.tagName);
			rsTagId = psTagId.executeQuery();
			
			
			// get the tag id, insert the income with this id
			long tagId;
			if (rsTagId.next()) {
				tagId = rsTagId.getLong(1);
				psInsIncome = connection.prepareStatement("insert into incomes (owner, amount, description, date_occur, scheduler, tag) "
						+ "values (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
				psInsIncome.setLong(1, income.owner);
				psInsIncome.setBigDecimal(2, income.amount);
				psInsIncome.setString(3, income.description);
				psInsIncome.setDate(4, new java.sql.Date(income.date_occur.getTime()));
				if (income.scheduler != null) {
					psInsIncome.setLong(5, income.scheduler);
				} else {
					psInsIncome.setNull(5, java.sql.Types.BIGINT);
				}
				
				psInsIncome.setLong(6, tagId);
				psInsIncome.executeUpdate();
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
				if (psInsIncome != null) {
					psInsIncome.close();
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
	 * gets the id of a record from the database with details matching the income object
	 * returns 
	 * 
	 * @return long id or -2 if no record found
	 */
	public long getId() {
		long incomeId = -2;
		
		Connection connection = DB.getConnection();
		
		PreparedStatement psSelectIncome = null;
		
		ResultSet rsSelectIncome = null;
		
		try {
			psSelectIncome = connection.prepareStatement("SELECT * FROM income WHERE owner = ? AND scheduler = ? " +
											"AND amount = ? AND description = ? AND date_occur = ? AND tag = ?");
			
			
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			
		}
		
		return incomeId;
	}
	
	/**
	 * 
	 * updates the database record with the id
	 * using the values from the income object
	 * 
	 * @param incomeId - Id of the income in the database
	 * @return success
	 */
	public boolean update(long incomeId) {
		boolean success =  true;
		
		Connection connection = DB.getConnection();
		
		PreparedStatement psSelectTags = null;
		PreparedStatement psInsertTags = null;
		PreparedStatement psUpdateIncome = null;
		
		ResultSet rsSelectTags = null;
		ResultSet rsTagKey = null;
		
		try {
			// insert any new tag if it does not exist
			psSelectTags = connection.prepareStatement("SELECT * FROM incomes_tags WHERE name = ?");
			psSelectTags.setString(1, this.tagName);
			rsSelectTags = psSelectTags.executeQuery();
			
			long tagId = 0;
			
			// if the tag does not exist create it
			if (rsSelectTags.next()) {
				// get existing tag
				tagId = rsSelectTags.getLong(1);
			} else {
				// create a n
				psInsertTags = connection.prepareStatement("INSERT INTO incomes_tags (owner, name) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
				psInsertTags.setLong(1, this.owner);
				psInsertTags.setString(2, this.tagName);
				psInsertTags.executeUpdate();
				rsTagKey = psInsertTags.getGeneratedKeys();
				tagId = rsTagKey.getLong(1);
			}
			
			// update the income database record
			psUpdateIncome = connection.prepareStatement("UPDATE incomes SET amount = ?, description = ?, date_occur = ?, tag = ? WHERE id = ?");
			psUpdateIncome.setBigDecimal(1, this.amount);
			psUpdateIncome.setString(2, this.description);
			psUpdateIncome.setDate(3, new java.sql.Date(this.date_occur.getTime()));
			psUpdateIncome.setLong(4, tagId);
			psUpdateIncome.setLong(5, tagId);
			psUpdateIncome.executeUpdate();
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
				
				if (psUpdateIncome != null) {
					psUpdateIncome.close();
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
	 * Get all the incomes belonging to specified user
	 * @param accId id number of the account from db
	 * @return
	 */
	public static List<Income> getIncomes(String accId) {
		List<Income> incomes = new ArrayList<Income>();
		// select all incomes by user
		/* select i.amount, i.desc, i.date_occur, it.name from income i 
		 *	 join incomes_tags_map im on i.id = im.income
		 *	 join incomes_tags it on im.tag = it.id  
		 *where i.owner = userId
		 * 
		 */ 
		
		Connection connection = DB.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			ps = connection.prepareStatement("select i.amount, i.description, i.date_occur, i.scheduler, it.name from incomes i"
					+ "	join incomes_tags it on i.tag = it.id where i.owner = ? order by i.date_occur desc;");
			ps.setLong(1, Long.parseLong(accId));
			rs = ps.executeQuery();
			
			while (rs.next()) {
				Income i = new Income(accId, rs.getBigDecimal("amount").toString(), rs.getString("name"), rs.getDate("date_occur").toString(), rs.getString("description"), (Long) rs.getLong("scheduler"));
				incomes.add(i);
			}
			
			// go through the list and sum up the incomes for each tag name
			// get list containing distinct tag names
			List<String> tagList = new ArrayList<String>();
			/*for (Income i: incomes) {
				if (!tagList.contains(i.tagName)) {
					tagList.add(i.tagName);
				}
			}*/
			
			// query sql to sum the income amount for us
			
			// TODO: this method should just return the list of incomes..
			// and a different method will take in that list and calculate the rest of the stuff.
			
			
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return incomes;
	}
		
	public static List<String> getTags(String accId) {
		List<String> tagList = new ArrayList<String>();
		
		Connection connection = DB.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			ps = connection.prepareStatement("SELECT name FROM incomes_tags WHERE owner = ?");
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
