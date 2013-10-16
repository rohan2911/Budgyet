package models;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import play.data.validation.Constraints.Required;
import play.data.validation.ValidationError;
import play.db.DB;

public class Expense {
	
	@Required
	public String amount;
	
	@Required
	public String description;
	
/*	@Required
	public String date;*/
	
	@Required
	public int year;
	
	@Required
	public int month;
	
	@Required
	public int day;
	
	public List<ValidationError> validate() {
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
		
/*		if (!date.matches("\\d{2}[-\\/]\\d{2}[-\\/]\\d{4}")) {
			errors.add(new ValidationError("expense date", "expense date provided not a valid date"));
		} else if (amount.length() > 10){
			errors.add(new ValidationError("expense date", "expense date provided not a valid date"));
		}*/
		
		
		
		
		return errors.isEmpty() ? null : errors;
	}
	
	public static boolean add(Expense expense) {
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
	}
	
}