package controllers;

import java.util.ArrayList;
import java.util.List;

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
    		
    		List<Income> incomes = Income.getIncomes(accId).subList(0, 5);	// store the first 5 incomes
    		List<Expense> expenses = Expense.getExpenses(accId).subList(0, 5);	// store the first 5 incomes
    		
    		return ok(home.render(Income.getTags(accId), Expense.getTags(accId), Budget.getProgressBudgets(accId),
    					incomes, expenses));
    	} else {
    		return ok(index.render());
    	}
    }
  
}
