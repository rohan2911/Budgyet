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
			
			/*
			
			generatedKeys = ps1.getGeneratedKeys();
			if (generatedKeys.next()) {
				long income_id = generatedKeys.getLong(1);
				
				// insert the tag
				// TODO: change this so that the income only has max. 1 tag
				ps2 = connection.prepareStatement("insert into incomes_tags (owner, name) select * from (select ?, ?) as tmp "
						+ "where not exists (select 1 from incomes_tags where owner = ? and name = ?)");
				ps2.setLong(1, income.owner);
				ps2.setLong(3, income.owner);
				
				ps2.setString(2, income.tag);
				ps2.setString(4, income.tag);
				ps2.executeUpdate();
				
				
				Iterator<String> tags_it = income.tags.iterator();
				while (tags_it.hasNext()) {
					String tag = tags_it.next();
					ps2.setString(2, income.tag);
					ps2.setString(4, income.tag);
					ps2.executeUpdate();
				}
				
				// get tag ids
				String sql3 = "select id from incomes_tags where owner = ? and (name = ?";
				for (int i=0; i<income.tags.size()-1; i++) {
					sql3 += " OR name = ?";
				}
				sql3 += ")";
				ps3 = connection.prepareStatement(sql3);
				ps3.setLong(1, income.owner);
				for (int i=0; i<income.tags.size(); i++) {
					ps3.setString(i+2, income.tags.get(i));
				}
				rs = ps3.executeQuery();

					
				// insert the income tag mapping
				ps4 = connection.prepareStatement("insert into incomes_tags_map (income, tag) values (?, ?)");
				ps4.setLong(1, income_id);
				while (rs.next()) {
					ps4.setLong(2, rs.getLong("id"));
					ps4.executeUpdate();
				}

					
			}
			*/
			
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
			ps = connection.prepareStatement(" select i.amount, i.description, i.date_occur, it.name from incomes i"
					+ "	join incomes_tags_map im on i.id = im.income"
					+ "	join incomes_tags it on im.tag = it.id"
					+ "	where i.owner = ?;");
			ps.setLong(1, Long.parseLong(accId));
			rs = ps.executeQuery();
			
			while(rs.next()) {
				Income i = new Income(accId, accId, accId, accId, accId, new Long(1));
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}

		
		return incomes;
	}
}
