package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;

import play.data.validation.Constraints.Required;
import play.db.ebean.Model;
import play.db.ebean.Model.Finder;

@Entity
public class Account extends Model {

	@Id
	public Long id;
	
	@Required
	public String email;
	
	@Required
	public String username;
	
	@Required
	public String password;
	
	public static Finder<Long,Account> find = new Finder<Long, Account>(Long.class, Account.class);
	
	public static List<Account> all() {
		return find.all();
	}
	
	public static boolean validateAccount(String username, String password) {
		return true;
	}
	
	public static void create(Account account) {
		account.save();
	}
	
	public static void delete(Long id) {
		find.ref(id).delete();
	}
	
}
