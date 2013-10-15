package models;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import play.data.validation.Constraints.Required;
import play.data.validation.ValidationError;
import play.db.DB;

public class Account {
	
	@Required
	public String username;
	
	@Required
	public String email;
	
	@Required
	public String password;
	
	
	public List<ValidationError> validate() {
		List<ValidationError> errors = new ArrayList<ValidationError>();
		
		// TODO CHECK EXISTANCE OF USERNAME AND PASSWORDS
		
		if (username.matches("[^\\w]")) {
			errors.add(new ValidationError("username", "username must contain only letters, digits and underscores"));
		} else if (username.length() < 5 || username.length() > 30) {
			errors.add(new ValidationError("username", "username must be between 5-30 characters long"));
		}
		
		if (!email.matches("(?i)^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}$")) {
			errors.add(new ValidationError("email", "invalid email"));
		}
		
		return errors.isEmpty() ? null : errors;
	}
	
	public static boolean authenticate(String username, String password) {
		Connection connection = DB.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		boolean success = false;
		
		try {
			ps = connection.prepareStatement("SELECT 1 FROM accounts WHERE username = ? AND password = ?");
			ps.setString(1, username);
			ps.setString(2, password);
			rs = ps.executeQuery();
			if (rs.next()) {
				success = true;
			}
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return success;
	}
	
	
	
	
	public static List<Account> all() {
		return new ArrayList<Account>();
	}
	
	
	
	public static boolean add(Account account) {
		Connection connection = DB.getConnection();
		PreparedStatement ps = null;
		boolean success = true;
		try {
			ps = connection.prepareStatement("INSERT INTO accounts (username, email, password) VALUES (?, ?, ?)");
			ps.setString(1, account.username);
			ps.setString(2, account.email);
			ps.setString(3, account.password);
			ps.executeUpdate();
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return success;
	}
	
	public static void delete(Long id) {
		return;
	}
	
}
