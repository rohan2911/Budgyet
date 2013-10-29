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
	public Long scheduler;	// id of the scheduler assigned to the income
	
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
		this.scheduler = (long) 0;
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
				if (income.scheduler != 0) {
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
		List<String> tagNameList = getIncomeTags(accId);
		Connection connection = DB.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		for (String tag: tagNameList) {
			try {
				ps  = connection.prepareStatement("select sum(amount) as total from incomes i join incomes_tags it "
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
	 * Gets the list of the owner's current income tags, but only those with existing incomes.
	 * Used for displaying the tags on the tag cost breakdown.
	 * @param accId user account's id
	 * @return list of all the tag names that the user owns
	 */
	public static List<String> getIncomeTags(String accId) {
		List<String> tagList = new ArrayList<String>();
		
		Connection connection = DB.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			ps = connection.prepareStatement("SELECT name FROM incomes_tags it "
					+ "WHERE it.owner = ? AND EXISTS (SELECT incomes.id FROM incomes WHERE tag = it.id)");
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
			ps = connection.prepareStatement("SELECT name FROM incomes_tags it WHERE it.owner = ?");
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
	 * Get the income with id incomeId from the database 
	 * 
	 * @param incomeId - the id of the income you wish to get
	 * @return Income populated with the income record
	 */
	public static Income get(long incomeId) {
		Income returnIncome = null;
		
		Connection connection = DB.getConnection();
		
		PreparedStatement psIncomeSelect = null;
		PreparedStatement psTagSelect = null;
		
		ResultSet rsIncomeSelect = null;
		ResultSet rsTagSelect = null;
		
		try {
			// select the income record
			psIncomeSelect = connection.prepareStatement("SELECT * FROM incomes WHERE id = ?");
			psIncomeSelect.setLong(1, incomeId);
			rsIncomeSelect = psIncomeSelect.executeQuery();
			
			if (rsIncomeSelect.next()) {
				
				// select the tag the income is attatched to
				psTagSelect = connection.prepareStatement("SELECT * FROM incomes_tags WHERE id = ?");
				psTagSelect.setLong(1, rsIncomeSelect.getLong("tag"));
				rsTagSelect = psTagSelect.executeQuery();
				
				if (rsTagSelect.next()) {
					// choose the constructor based on if the income is tied to a scheduler 
					rsIncomeSelect.getLong("scheduler");
					if (rsIncomeSelect.wasNull()) {
						returnIncome = new Income(rsIncomeSelect.getString("owner"), rsIncomeSelect.getString("amount"),
								rsTagSelect.getString("name"), rsIncomeSelect.getString("date_occur"),
								rsIncomeSelect.getString("description"));
					} else {
						returnIncome = new Income(rsIncomeSelect.getString("owner"), rsIncomeSelect.getString("amount"),
								rsTagSelect.getString("name"), rsIncomeSelect.getString("date_occur"),
								rsIncomeSelect.getString("description"), rsIncomeSelect.getLong("scheduler"));
					}
				}
				returnIncome.id = incomeId;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (rsIncomeSelect != null) {
					rsIncomeSelect.close();
				}
				
				if (rsTagSelect != null) {
					rsTagSelect.close();
				}
				
				if (psIncomeSelect != null) {
					psIncomeSelect.close();
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
		
		return returnIncome;
	}
	
	/**
	 * 
	 * Checks whether the scheduler for this income exists based on the income id
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
			psSchedulerSelect = connection.prepareStatement("SELECT scheduler FROM incomes WHERE id = ?");
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
	 * removes the income at id from the database
	 * 
	 * @returns success
	 */
	
	public static boolean remove(long id) {
		boolean success = true;
		
		Connection connection = DB.getConnection();
		
		PreparedStatement psIncomeDelete = null;
		
		try {
			psIncomeDelete = connection.prepareStatement("DELETE FROM incomes WHERE id = ?");
			psIncomeDelete.setLong(1, id);
			psIncomeDelete.executeUpdate();
		} catch (SQLException e) {
			success = false;
			e.printStackTrace();
		} finally {
			try {
				if (psIncomeDelete != null) {
					psIncomeDelete.close();
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
