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
import java.util.Date;

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
	 * @param tag the tag associated with this expense
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
				
			} else {
				// this should never happen
			}

			/*
			generatedKeys = ps1.getGeneratedKeys();
			if (generatedKeys.next()) {
				long expense_id = generatedKeys.getLong(1);

				// insert the tags
				ps2 = connection.prepareStatement("insert into expenses_tags (owner, name) select * from (select ?, ?) as tmp where "
						+ "not exists (select 1 from expenses_tags where owner = ? and name = ?)");
				ps2.setLong(1, expense.owner);
				ps2.setLong(3, expense.owner);
				Iterator<String> tags_it = expense.tagName.iterator();
				while (tags_it.hasNext()) {
					String tag = tags_it.next();
					ps2.setString(2, tag);
					ps2.setString(4, tag);
					ps2.executeUpdate();
				}
				
				// get tag ids
				String sql3 = "select id from expenses_tags where owner = ? and (name = ?";
				for (int i=0; i<expense.tagName.size()-1; i++) {
					sql3 += " OR name = ?";
				}
				sql3 += ")";
				ps3 = connection.prepareStatement(sql3);
				ps3.setLong(1, expense.owner);
				for (int i=0; i<expense.tagName.size(); i++) {
					ps3.setString(i+2, expense.tagName.get(i));
				}
				rs = ps3.executeQuery();
				
				// insert the expense tag mapping
				ps4 = connection.prepareStatement("insert into expenses_tags_map (expense, tag) values (?, ?)");
				ps4.setLong(1, expense_id);
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
	
}