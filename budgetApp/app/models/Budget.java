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

/**
 * Budget model
 * @author Hana
 *
 */
public class Budget {

	public long owner;
	public String title;
	public BigDecimal amount;
	public List<String> tags;
	public Date date_start;
	public Date date_end;
	public String description;

	/**
	 * Constructor
	 */
	public Budget(String owner, String title, String amount, String tags, String date_start, String date_end, String description) {
		this.owner = Long.parseLong(owner);
		this.title = title;
		this.amount = new BigDecimal(amount).setScale(2, RoundingMode.HALF_UP);
		this.tags = new ArrayList<String>(Arrays.asList(tags.split(",")));
		try {
			// simpledateformat is a JAVA date. 
			this.date_start = new SimpleDateFormat("yyyy-MM-dd").parse(date_start);
			this.date_end = new SimpleDateFormat("yyyy-MM-dd").parse(date_end);
		} catch (ParseException e) {
			this.date_start= null;
			this.date_end= null;
			e.printStackTrace();
		}
		this.description = description;
	}

	/**
	 * Adds this budget to the db
	 * @return
	 */
	public static boolean add(Budget budget) {
		Connection connection = DB.getConnection();
		PreparedStatement ps1 = null;
		PreparedStatement ps2 = null;
		PreparedStatement ps3 = null;
		PreparedStatement ps4 = null;
		ResultSet generatedKeys = null;
		ResultSet rs = null;
		
		try {
			// insert the budget
			ps1 = connection.prepareStatement("insert into budgets (owner, title, amount, description, date_start, date_end) values (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			ps1.setLong(1, budget.owner);
			ps1.setString(2, budget.title);
			ps1.setBigDecimal(3, budget.amount);
			ps1.setString(4, budget.description);
			ps1.setDate(5, new java.sql.Date(budget.date_start.getTime()));
			ps1.setDate(6, new java.sql.Date(budget.date_end.getTime()));
			ps1.executeUpdate();
			
			generatedKeys = ps1.getGeneratedKeys();
			if (generatedKeys.next()) {
				long budget_id = generatedKeys.getLong(1);
				
				// insert the tags
				ps2 = connection.prepareStatement("insert into expenses_tags (owner, name) select * from (select ?, ?) as tmp where "
						+ "not exists (select 1 from expenses_tags where owner = ? and name = ?)");
				ps2.setLong(1, budget.owner);
				ps2.setLong(3, budget.owner);
				Iterator<String> tags_it = budget.tags.iterator();
				while (tags_it.hasNext()) {
					String tag = tags_it.next();
					ps2.setString(2, tag);
					ps2.setString(4, tag);
					ps2.executeUpdate();
				}
				
				// get tag ids
				String sql3 = "select id from expenses_tags where owner = ? and (name = ?";
				for (int i=0; i<budget.tags.size()-1; i++) {
					sql3 += " OR name = ?";
				}
				sql3 += ")";
				ps3 = connection.prepareStatement(sql3);
				ps3.setLong(1, budget.owner);
				for (int i=0; i<budget.tags.size(); i++) {
					ps3.setString(i+2, budget.tags.get(i));
				}
				rs = ps3.executeQuery();
				
				// insert the expense tag mapping
				ps4 = connection.prepareStatement("insert into budgets_tags_map (budget, tag) values (?, ?)");
				ps4.setLong(1, budget_id);
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


