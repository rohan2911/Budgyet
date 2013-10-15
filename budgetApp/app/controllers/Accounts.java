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
    	final DynamicForm form = DynamicForm.form().bindFromRequest();
    	final String username = form.get("username");
    	final String password = form.get("password");
    	
    	if (Account.authenticate(username, password)) {
    		session("connected", username);
    		return ok(bonus.render());
    	} else {
    		flash("fail", "incorrect username or password");
    		return redirect(routes.Application.index());
    	}
    }
    

    public static Result logout() {
    	// destroy the session and return to index
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