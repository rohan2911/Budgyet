package controllers;

import models.Account;
import play.data.DynamicForm;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.*;

/**
 * Controls the account objects.
 * @author Rohan, Leslie, Hana, Tracey, Jeremy
 */
public class Accounts extends Controller {

	/**
	 * Logs the user in, with the username and password values from
	 * the forms.
	 * @return redirects to index page.
	 */
    public static Result login() {
    	
    	// retrieve form data
    	DynamicForm form = DynamicForm.form().bindFromRequest();
    	String username = form.get("username");
    	String password = form.get("password");
    	
    	long id = Account.authenticate(username, password);
    	
    	if (id != 0) {
    		session("connected_username", username);
    		session("connected_id", Long.toString(id));
    		//return redirect(routes.Application.index());
    		return redirect(routes.Application.index());
    	} else {
    		flash("login_fail", "invalid username or password");
    		return redirect(routes.Application.index());
    	}
    }
    
    /**
     * Logs the user out.
     * @return clears the session and redirects to the index page.
     */
    public static Result logout() {
    	session().clear();
    	return redirect(routes.Application.index());
    }
    
    
    /**
     * Displays the register page.
     */
    public static Result register() {
    	Form<Account> registerForm = Form.form(Account.class);
    	return ok(register.render(registerForm));
    }
	
    /**
     * Adds the account to the system. 
     * @return redirects to the index page.
     */
    public static Result addAccount() {
    	
    	Form<Account> accountForm = Form.form(Account.class);
    	accountForm = accountForm.bindFromRequest();
    	if (accountForm.hasErrors()) {
    		return badRequest(register.render(accountForm));
    	} else {
    		Account.add(accountForm.get());
    		return redirect(routes.Application.index());
    	}
    }
}
