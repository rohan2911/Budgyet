package controllers;

import java.util.ArrayList;
import java.util.List;

import models.Budget;
import models.Expense;
import models.Income;
import play.mvc.*;
import views.html.*;

/**
 * Controls loading the index page.
 * @author Rohan, Leslie, Tracey, Jeremy, Hana
 *
 */
public class Application extends Controller {
	
	/**
	 * Displays the index page containing appropriate data;
	 *  includes list of budgets and submists of recent incomes and expenses.
	 * @return
	 */
    public static Result index() {
    	String username = session("connected_username");
    	if (username != null) {
    		String accId = session("connected_id");
    		
    		List<Income> incomes = Income.getIncomes(accId);	
    		List<Expense> expenses = Expense.getExpenses(accId);	
    		
    		if (incomes.size() > 6) {
    			incomes = incomes.subList(0, 5);
    		}
    		
    		if (expenses.size() > 6) {
    			expenses = expenses.subList(0, 5);
    		}
    		
    		return ok(home.render(Income.getTags(accId), Expense.getTags(accId), Budget.getProgressBudgets(accId),
    					incomes, expenses));
    	} else {
    		return ok(index.render());
    	}
    }
  
}
