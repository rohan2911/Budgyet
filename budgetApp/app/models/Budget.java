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
	 * @param owner owner's id
	 * @param title the budget title
	 * @param amount amount allocated for this budget
	 * @param tags list of tags associated with this budget, in one string, separated by commas
	 * @param date_start starting date of the budget
	 * @param date_end ending date of the budget
	 * @param description budget description
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
	 * @param budget the budget object containing the information to be added to db.
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
	

	/**
	 * Fetches the list of budgets for the specified owner.
	 * @param owner the id of the owner to get the budgets of
	 * @return List of BudgetBar, which is just a class that holds budget information.
	 */
	public static List<BudgetBar> getProgressBudgets(String owner) {
		
		/*
		 * return list of budgets for the user
		 * for each budget,
		 * 		get its associated tags, 
		 * 		get expenses with those tags, 
		 * 		for each expense amount, add to total $ spent
		 * 		then get total expenditure
		 * 
		 * select * from budgets where owner = owner
		 * 		
		 */
		Connection connection = DB.getConnection();
		PreparedStatement ps1 = null;
		PreparedStatement ps2 = null;
		PreparedStatement ps3 = null;
		ResultSet rs1 = null;
		ResultSet rs2 = null;
		ResultSet rs3 = null;
		
		List<BudgetBar> budgetBars = new ArrayList<BudgetBar>();
		
		try {
			ps1 = connection.prepareStatement("select * from budgets where owner = ?");
			ps1.setLong(1, Long.parseLong(owner));
			rs1 = ps1.executeQuery();
			// fill up a list of BudgetBars here to give to play
			while(rs1.next()) {
				BudgetBar bb = new BudgetBar();
				bb.setId(rs1.getLong("id"));
				bb.setAmount(rs1.getBigDecimal("amount"));
				bb.setDateStart(rs1.getDate("date_start").toString());
				bb.setDateEnd(rs1.getDate("date_end").toString());
				bb.setTitle(rs1.getString("title"));
				
				// need to calculate progress here for the budget
				// 1. get this budget's tags
				// select tag from budgets_tags_map join expenses_tags on tag = id where budget = '2';
				ps2 = connection.prepareStatement("select tag from budgets_tags_map "
						+ "join expenses_tags on tag = id where budget = ?;");
				ps2.setLong(1, bb.getId());
				rs2 = ps2.executeQuery();
				
				// get list of tag ids
				List<Long> tag_ids = new ArrayList<Long>();
				while (rs2.next()) {
					// has tag ids
					tag_ids.add(rs2.getLong("tag"));
				}
				
				// get all the expenses tag mapping with given tag ids
//				String sql3 = "select amount from expenses join expenses_tags_map on id = expense where (tag = ?";
				String sql3 = "select amount from expenses where (tag = ?";
				for (int i=0; i<tag_ids.size()-1; i++) {
					sql3 += " OR tag = ?";
				}
				sql3 += ") and date_occur >= ? and date_occur <= ?";
				ps3 = connection.prepareStatement(sql3);
				
				// fill the preparedstatement in with the tag ids, then execute
				int i=1;
				for (; i<=tag_ids.size(); i++) {
					ps3.setLong(i, tag_ids.get(i-1));
				}
				ps3.setString(i, bb.getDateStart());
				ps3.setString(i+1, bb.getDateEnd());
				rs3 = ps3.executeQuery();
				
				// sum up all the expense amounts
				BigDecimal totalAmt = new BigDecimal("0.00").setScale(2, RoundingMode.HALF_UP);
				while(rs3.next()) {
					totalAmt = totalAmt.add(rs3.getBigDecimal("amount"));
				}
				bb.setProgress(totalAmt);
				
				// insert into list
				budgetBars.add(bb);
			}
			
		} catch (SQLException e) {
//			success = false;
			e.printStackTrace();
		} finally {
			try {
				if (rs1 != null) {
					rs1.close();
				}
				if (rs2 != null) {
					rs2.close();
				}
				if (rs3 != null) {
					rs3.close();
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
				if (connection != null) {
					connection.close();
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return budgetBars;
	}
}


