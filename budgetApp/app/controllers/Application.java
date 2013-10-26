package controllers;

import models.Budget;
import models.Expense;
import models.Income;
import play.mvc.*;
import views.html.*;

public class Application extends Controller {
	
    public static Result index() {
    	String username = session("connected_username");
    	if (username != null) {
    		String accId = session("connected_id");
    		return ok(home.render(Income.getTags(accId), Expense.getTags(accId), Budget.getProgressBudgets(accId)));
    	} else {
    		return ok(index.render());
    	}
    }
  
}
