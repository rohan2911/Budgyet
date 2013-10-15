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
	public String value;
	
	@Required
	public String name;
	
	@Required
	public String date;
	
	public List<ValidationError> validate() {
		List<ValidationError> errors = new ArrayList<ValidationError>();
		
		// check if vlaues are set
		
		if (!name.matches("[^\\w]")) {
			errors.add(new ValidationError("expense name", "expense name must contain only letters, digits and underscores"));
		} else if (name.length() > 128){
			errors.add(new ValidationError("expense name", "expense name must contain less than 128 characters"));
		}
		
		if (!value.matches("\\d*\\.??\\d{0,2}")) {
			errors.add(new ValidationError("expense amount", "expense amount provided not a valid amount"));
		} else if (value.length() > 20){
			errors.add(new ValidationError("expense amount", "expense amount is too large"));
		}
		
		if (!date.matches("\\d{2}[-\\/]\\d{2}[-\\/]\\d{4}")) {
			errors.add(new ValidationError("expense date", "expense date provided not a valid date"));
		} else if (value.length() > 10){
			errors.add(new ValidationError("expense date", "expense date provided not a valid date"));
		}
		
		
		return errors.isEmpty() ? null : errors;
	}
	
	public static boolean add(Expense expense) {
		boolean success = true;
		Connection connection = DB.getConnection();
		
		PreparedStatement ps = null;
		
		try {
			ps = connection.prepareStatement("INSERT INTO expenses (name, value, date) VALUES (?,?,?)");
			ps.setString(1, expense.name);
			ps.setBigDecimal(2, new BigDecimal(expense.value));
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