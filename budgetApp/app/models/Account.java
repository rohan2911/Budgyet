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

/**
 * The Account class handles all the logic involved in handling account information.
 * @author Rohan, Leslie, Tracey, Jeremy, Hana
 *
 */
public class Account {
	
	@Required
	private String username;
	
	@Required
	private String email;
	
	@Required
	private String password;
	
	@Required
	private String password_check;
	
	private String first_name;
	private String last_name;
	
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPassword_check() {
		return password_check;
	}

	public void setPassword_check(String password_check) {
		this.password_check = password_check;
	}

	public String getFirst_name() {
		return first_name;
	}

	public void setFirst_name(String first_name) {
		this.first_name = first_name;
	}

	public String getLast_name() {
		return last_name;
	}

	public void setLast_name(String last_name) {
		this.last_name = last_name;
	}

	/**
	 * Form validator. Checks for certain constraints on each forms,
	 * and assigns the appropriate error. 
	 * @return list of errors if there are any.
	 */
	public List<ValidationError> validate() {
		List<ValidationError> errors = new ArrayList<ValidationError>();
		
		// TODO CHECK EXISTANCE OF USERNAME AND PASSWORDS
		
		if (username.matches("[^\\w]")) {
			errors.add(new ValidationError("username", "username must contain only letters, digits and underscores"));
		} else if (username.length() < 5 || username.length() > 30) {
			errors.add(new ValidationError("username", "username must be between 5-30 characters long"));
		} else if (!uniqueUsername(username)) {
			errors.add(new ValidationError("username", "this username is already in use"));
		}
		
		if (!email.matches("(?i)^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}$")) {
			errors.add(new ValidationError("email", "invalid email"));
		} else if (!uniqueEmail(email)) {
			errors.add(new ValidationError("email", "this email is already registered"));
		}
		
		if (password.length() < 6 || username.length() > 30) {
			errors.add(new ValidationError("password", "password must be between 6-30 characters long"));
		} else if (!password.equals(password_check)) {
			errors.add(new ValidationError("password_check", "password did not match!"));
		}
		
		if (!first_name.matches("^[A-Za-z\\s-]*$")) {
			errors.add(new ValidationError("first_name", "first name must contain only letters, spaces and hyphens"));
		}
		
		if (!last_name.matches("^[A-Za-z\\s-]*$")) {
			errors.add(new ValidationError("last_name", "last name must contain only letters, spaces and hyphens"));
		}
		
		return errors.isEmpty() ? null : errors;
	}
	
	/**
	 * Adds an account to the db.
	 * @param account
	 * @return true if successfully added to the database, false otherwise
	 */
	public static boolean add(Account account) {
		Connection connection = DB.getConnection();
		PreparedStatement ps = null;
		boolean success = true;
		try {
			ps = connection.prepareStatement("INSERT INTO accounts (username, email, password, first_name, last_name) VALUES (?, ?, ?, ?, ?)");
			ps.setString(1, account.username);
			ps.setString(2, account.email);
			ps.setString(3, account.password);
			ps.setString(4, account.first_name);
			ps.setString(5, account.last_name);
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

	/**
	 * Authenticates the user against the password
	 * @param username
	 * @param password
	 * @return 0 if not found, else the id of the account
	 */
	public static long authenticate(String username, String password) {
		Connection connection = DB.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		long id = 0;
		
		try {
			ps = connection.prepareStatement("SELECT id FROM accounts WHERE username = ? AND password = ?");
			ps.setString(1, username);
			ps.setString(2, password);
			rs = ps.executeQuery();
			if (rs.next()) {
				id = rs.getLong("id");
			}
		} catch (SQLException e) {
			id = 0;
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
		return id;
	}

	/**
	 * Checks if the given username is unique. i.e. the username does not currently exist in the database.
	 * @param username
	 * @return true if unique username, false otherwise
	 */
	public static boolean uniqueUsername(String username) {
		Connection connection = DB.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		boolean unique = true;
		
		try {
			ps = connection.prepareStatement("SELECT 1 FROM accounts WHERE username = ?");
			ps.setString(1, username);
			rs = ps.executeQuery();
			if (rs.next()) {
				unique = false;
			}
		} catch (SQLException e) {
			unique = false;
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
		return unique;
	}
	
	/**
	 * Checks if email is unique in the database/already being used
	 * @param email
	 * @return true if email is unique, false otherwise
	 */
	public static boolean uniqueEmail(String email) {
		Connection connection = DB.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		boolean unique = true;
		
		try {
			ps = connection.prepareStatement("SELECT 1 FROM accounts WHERE email = ?");
			ps.setString(1, email);
			rs = ps.executeQuery();
			if (rs.next()) {
				unique = false;
			}
		} catch (SQLException e) {
			unique = false;
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
		return unique;
	}

	
}
