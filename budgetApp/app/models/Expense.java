package models;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import play.data.validation.Constraints.Required;
import play.data.validation.ValidationError;
import play.db.DB;

public class Expense {
	
	public long owner;
	
	public BigDecimal amount;
	public List<String> tags;
	public Date income_date;
	public String description;
	public Boolean repeating;
	public 
	
	public Expense(String owner, String amount, String tags, String date, String description) {
		this.owner = Long.parseLong(owner);
		this.amount = new BigDecimal(amount).setScale(2, RoundingMode.HALF_UP);
		this.tags = new ArrayList<String>(Arrays.asList(tags.split(",")));
		try {
			this.income_date = new SimpleDateFormat("yyyy-MM-dd").parse(date);
		} catch (ParseException e){
			this.income_date = null;
			e.printStackTrace();
		}
		this.description = description;
	}
	
/*	public List<ValidationError> validate() {
		List<ValidationError> errors = new ArrayList<ValidationError>();
		
		// check if amounts are set
		
		if (!description.matches("[^\\w]")) {
			errors.add(new ValidationError("expense description", "expense description must contain only letters, digits and underscores"));
		} else if (description.length() > 128){
			errors.add(new ValidationError("expense description", "expense description must contain less than 128 characters"));
		}
		
		if (!amount.matches("\\d*\\.??\\d{0,2}")) {
			errors.add(new ValidationError("expense amount", "expense amount provided not a valid amount"));
		} else if (amount.length() > 20){
			errors.add(new ValidationError("expense amount", "expense amount is too large"));
		}
		
		if (!date.matches("\\d{2}[-\\/]\\d{2}[-\\/]\\d{4}")) {
			errors.add(new ValidationError("expense date", "expense date provided not a valid date"));
		} else if (amount.length() > 10){
			errors.add(new ValidationError("expense date", "expense date provided not a valid date"));
		}	
		
		
		
		return errors.isEmpty() ? null : errors;
	}*/
	
/*	public static boolean add(Expense expense) {
		boolean success = true;
		Connection connection = DB.getConnection();
		
		PreparedStatement ps = null;
		
		try {
			ps = connection.prepareStatement("INSERT INTO expenses (description, amount, date) amountS (?,?,?)");
			ps.setString(1, expense.description);
			ps.setBigDecimal(2, new BigDecimal(expense.amount));
			ps.setString(3, expense.date);
			ps.executeQuery();
			
		} catch (SQLException e) {
			success = false;
			e.printStackTrace();
		} finally {
			try {
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
		return success;
	}*/
	
}