package controllers;

import models.Account;
import play.data.DynamicForm;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.*;

public class Accounts extends Controller {

    public static Result login() {
    	
    	// retrieve form data
    	DynamicForm form = DynamicForm.form().bindFromRequest();
    	String username = form.get("username");
    	String password = form.get("password");
    	
    	long id = Account.authenticate(username, password);
    	
    	if (id != 0) {
    		session("connected_username", username);
    		session("connected_id", Long.toString(id));
    		// TODO go somewhere useful
    		//return redirect(routes.Application.index());
    		return ok(home.render());
    	} else {
    		flash("fail", "invalid username or password");
    		return redirect(routes.Application.index());
    	}
    }
    
    public static Result logout() {
    	session().clear();
    	return redirect(routes.Application.index());
    }
    
    
    
    public static Result register() {
    	Form<Account> registerForm = Form.form(Account.class);
    	return ok(register.render(registerForm));
    }
	
    public static Result addAccount() {
    	
    	Form<Account> accountForm = Form.form(Account.class);
    	accountForm = accountForm.bindFromRequest();
    	if (accountForm.hasErrors()) {
    		return badRequest(register.render(accountForm));
    	} else {
    		Account.add(accountForm.get());
    		// TODO add a flash for message indicating successful registration
    		return redirect(routes.Application.index());
    	}
    }
}
