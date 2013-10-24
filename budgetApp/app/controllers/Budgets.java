package controllers;

import models.Budget;
import play.data.DynamicForm;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.*;

/**
 * Controls budgets
 * @author Hana
 *
 */
public class Budgets extends Controller {

	public static Result addBudget() {
		
		DynamicForm form = DynamicForm.form().bindFromRequest();
		String amount = form.get("budget_amount");
		String title = form.get("budget_title");
    	String tags = form.get("budget_tag_list");
    	String date_start = form.get("budget_start_date");
    	String date_end = form.get("budget_end_date");
    	String description = form.get("budget_description");
		
    	String repeat = form.get("budget_repeat");
		
    	Budget budget = new Budget(session().get("connected_id"), title, amount, tags, date_start, date_end, description);
    	Budget.add(budget);
    	
		return redirect(routes.Application.index());
	}
	
	public static Result budgets() {
		return ok(budgets.render(Budget.getProgressBudgets(session("connected_id"))));
	}
	
}
