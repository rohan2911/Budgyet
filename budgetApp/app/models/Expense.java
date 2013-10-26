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
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import play.db.DB;

public class Expense {
	
	public long owner;
	public BigDecimal amount;
	public List<String> tags;
	public Date date_occur;
	public String description;
	public long scheduler;
	
	public Expense(String owner, String amount, String tags, String date_occur, String description, long scheduler) {
		this.owner = Long.parseLong(owner);
		this.amount = new BigDecimal(amount).setScale(2, RoundingMode.HALF_UP);
		this.tags = new ArrayList<String>(Arrays.asList(tags.split(",")));
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
	
	public static boolean add(Expense expense) {
		
		Connection connection = DB.getConnection();
		PreparedStatement ps1 = null;
		PreparedStatement ps2 = null;
		PreparedStatement ps3 = null;
		PreparedStatement ps4 = null;
		ResultSet generatedKeys = null;
		ResultSet rs = null;
		
		try {
			// insert the expense
			ps1 = connection.prepareStatement("insert into expenses (owner, amount, description, date_occur, scheduler) values (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			ps1.setLong(1, expense.owner);
			ps1.setBigDecimal(2, expense.amount);
			ps1.setString(3, expense.description);
			ps1.setDate(4, new java.sql.Date(expense.date_occur.getTime()));
			ps1.setLong(5, expense.scheduler);
			ps1.executeUpdate();
			
			generatedKeys = ps1.getGeneratedKeys();
			if (generatedKeys.next()) {
				long expense_id = generatedKeys.getLong(1);
				
				// insert the tags
				ps2 = connection.prepareStatement("insert into expenses_tags (owner, name) select * from (select ?, ?) as tmp where "
						+ "not exists (select 1 from expenses_tags where owner = ? and name = ?)");
				ps2.setLong(1, expense.owner);
				ps2.setLong(3, expense.owner);
				Iterator<String> tags_it = expense.tags.iterator();
				while (tags_it.hasNext()) {
					String tag = tags_it.next();
					ps2.setString(2, tag);
					ps2.setString(4, tag);
					ps2.executeUpdate();
				}
				
				// get tag ids
				String sql3 = "select id from expenses_tags where owner = ? and (name = ?";
				for (int i=0; i<expense.tags.size()-1; i++) {
					sql3 += " OR name = ?";
				}
				sql3 += ")";
				ps3 = connection.prepareStatement(sql3);
				ps3.setLong(1, expense.owner);
				for (int i=0; i<expense.tags.size(); i++) {
					ps3.setString(i+2, expense.tags.get(i));
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
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (generatedKeys != null) {
					generatedKeys.close();
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return true;
	}
	
}