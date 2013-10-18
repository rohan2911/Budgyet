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

public class Income {

	public long owner;
	public BigDecimal amount;
	public List<String> tags;
	public Date income_date;
	public String description;
	
	public Income(String owner, String amount, String tags, String date, String description) {
		this.owner = Long.parseLong(owner);
		this.amount = new BigDecimal(amount).setScale(2, RoundingMode.HALF_UP);
		this.tags = new ArrayList<String>(Arrays.asList(tags.split(",")));
		try {
			this.income_date = new SimpleDateFormat("yyyy-MM-dd").parse(date);
		} catch (ParseException e) {
			this.income_date = null;
			e.printStackTrace();
		}
		this.description = description;
	}
	
	
	public static boolean add(Income income) {
		
		Connection connection = DB.getConnection();
		PreparedStatement ps1 = null;
		PreparedStatement ps2 = null;
		PreparedStatement ps3 = null;
		PreparedStatement ps4 = null;
		ResultSet generatedKeys = null;
		ResultSet rs = null;
		
		try {
			// insert the income
			ps1 = connection.prepareStatement("insert into incomes (owner, amount, description, income_date) values (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			ps1.setLong(1, income.owner);
			ps1.setBigDecimal(2, income.amount);
			ps1.setString(3, income.description);
			ps1.setDate(4, new java.sql.Date(income.income_date.getTime()));
			ps1.executeUpdate();
			
			generatedKeys = ps1.getGeneratedKeys();
			if (generatedKeys.next()) {
				long income_id = generatedKeys.getLong(1);
				
				// insert the tags
				ps2 = connection.prepareStatement("insert into income_tags (owner, name) select * from (select ?, ?) as tmp where not exists (select 1 from income_tags where owner = ? and name = ?)");
				ps2.setLong(1, income.owner);
				ps2.setLong(3, income.owner);
				Iterator<String> tags_it = income.tags.iterator();
				while (tags_it.hasNext()) {
					String tag = tags_it.next();
					ps2.setString(2, tag);
					ps2.setString(4, tag);
					ps2.executeUpdate();
				}
				
				// get tag ids
				String sql3 = "select id from income_tags where owner = ? and (name = ?";
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
				ps4 = connection.prepareStatement("insert into income_tags_map (income, tag) values (?, ?)");
				ps4.setLong(1, income_id);
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