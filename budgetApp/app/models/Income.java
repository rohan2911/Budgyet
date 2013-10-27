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

	public long id;
	public long owner;
	public BigDecimal amount;
//	public List<String> tags;
	public String tagName;	// max. 1 tag per income 
	public Date date_occur;
	public String date_display;
	public String description;
	public Long scheduler;	// id of the schedular assignmed to the income
	public int period;	// time period of the schedule
	
	/**
	 * Contructor for Income class.
	 * @param owner id of the owner of this income. (the current logged in user's id is passed in)
	 * @param amount the amount of income
	 * @param tagName the user specified tag name associated with this income. (only 1 allowed)
	 * @param date_occur user specified date of the income
	 * @param description income description
	 * @param scheduler id of the scheduler used for repeating incomes
	 */
	public Income(String owner, String amount, String tagName, String date_occur, String description, long scheduler) {
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
		this.period = 0;
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
		this.date_display = date;
		try {
			this.date_occur = new SimpleDateFormat("yyyy-MM-dd").parse(date);
		} catch (ParseException e) {
			this.date_occur = null;
			e.printStackTrace();
		}
		this.description = desc;
		this.scheduler = (Long) null;
		this.period = 0;
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
	 * Get all the incomes belonging to specified user.
	 * Used in displaying the list of incomes for the user (not the pie chart)
	 * @param accId id number of the account from db
	 * @return list of Incomes owned by the specified user.
	 */
	public static List<Income> getIncomes(String accId) {
		List<Income> incomes = new ArrayList<Income>();
		// select all incomes by user
		Connection connection = DB.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			ps = connection.prepareStatement("select i.id, i.amount, i.description, i.date_occur, i.scheduler, it.name from incomes i"
					+ "	join incomes_tags it on i.tag = it.id where i.owner = ? order by i.date_occur desc;");
			ps.setLong(1, Long.parseLong(accId));
			rs = ps.executeQuery();
			
			while (rs.next()) {
				Income i = new Income(accId, rs.getBigDecimal("amount").toString(), rs.getString("name"), 
						rs.getDate("date_occur").toString(), rs.getString("description"), (Long) rs.getLong("scheduler"));
				i.id = rs.getLong("id");
//				System.out.println("created:"+i.date_occur);
				incomes.add(i);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return incomes;
	}
	
	/**
	 * @param accId user account id as stored in the db. 
	 * @return the list of summed values of the tag names(owned by the user) found by getTags().
	 */
	public static List<String> getTagSum(String accId) {
		List<String> tagSums = new ArrayList<String>();
		List<String> tagNameList = getTags(accId);
		Connection connection = DB.getConnection();
		for (String tag: tagNameList) {
			try {
				PreparedStatement ps = connection.prepareStatement("select sum(amount) as total from incomes i join incomes_tags it "
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
		tags = tags.substring(0, tags.length()-1);
		return tags;
	}

	/**
	 * @param incId
	 * @return the income by its id
	 */
	public static Income getById(Long accId, Long incId) {
		Connection connection = DB.getConnection();
		PreparedStatement ps = null;
		PreparedStatement psSch = null;
		ResultSet rs = null;
		ResultSet rsSch = null;
		
		Income income = null;
		
		try {
			ps = connection.prepareStatement("select * from incomes i join incomes_tags it on i.tag = it.id where i.id = ? and i.owner = ?");
			ps.setLong(1, incId);
			ps.setLong(2, accId);
			rs = ps.executeQuery();
			
			if (rs.next()) {
				income = new Income(accId.toString(), rs.getBigDecimal("amount").toString(), rs.getString("name"), 
						rs.getDate("date_occur").toString(), rs.getString("description"), (Long) rs.getLong("scheduler"));
				income.id = incId;
				System.out.println("MY TAG:"+rs.getString("name"));
//				System.out.println(income.amount);
				if ((Long) rs.getLong("scheduler") != null) {
					System.out.println("I HAVE A SCHEDULER:"+rs.getLong("scheduler"));
					psSch = connection.prepareStatement("select period from scheduled_incomes where id = ?");
					psSch.setLong(1, rs.getLong("scheduler"));
					rsSch = psSch.executeQuery();
					if (rsSch.next()) {
						income.period = rsSch.getInt("period");
					}
				} else {
					income.period = 0;
				}
			}
			
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
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
		
		
		return income;
	}
}
