package controllers;

import java.util.List;

import models.Budget;
import models.Expense;
import models.Income;
import play.data.DynamicForm;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.*;

/**
 * Controls budgets
 * @author Rohan, Leslie, Tracey, Jeremy, Hana
 *
 */
public class Budgets extends Controller {

	/**
	 * Adds the budget to the system. 
	 * @return redirects to the budgets page if the user is logged in,
	 * 			otherwise redirects to the index page.
	 */
	public static Result addBudget() {
		String username = session("connected_username");
		if (username != null) {
			DynamicForm form = DynamicForm.form().bindFromRequest();
			String amount = form.get("budget_amount");
			String title = form.get("budget_title");
	    	String tags = form.get("budget_tag_list");
	    	String date_start = form.get("budget_start_date");
	    	String date_end = form.get("budget_end_date");
	    	String description = form.get("budget_description");
	    	String source = form.get("source");
	    	
//	    	System.out.println("source = " + source);
			
//	    	String repeat = form.get("budget_repeat");
			
	    	Budget budget = new Budget(session().get("connected_id"), title, amount, tags, date_start, date_end, description);
	    	Budget.add(budget);
	    	
			if (source.equals("budgets")) {
				return redirect(routes.Budgets.budgets());
			} else {
				return redirect(routes.Application.index());
			}
		}
		return redirect(routes.Application.index());
	}
	
	/**
	 * Displays the list of budgets owned by the user.
	 * @return If the user is logged in, loads the page which displays the user's budgets, 
	 * 			otherwise redirects to the index page.
	 */
	public static Result budgets() {
		String username = session("connected_username");
		if (username != null) {
			List<String> expenseTagNames = Expense.getExpenseTags(session().get("connected_id"));	// get list of expense tag names for graph
			return ok(budgets.render(Budget.getProgressBudgets(session("connected_id")), expenseTagNames));
		}
		return redirect(routes.Application.index());
	}
	
	/**
	 * Removes the specified budget from the system.
	 * @param id id of the budget passed from the URL
	 * @return If user is logged in, they are redirected to the budgets page,
	 * 			otherwise redirects to the index page.
	 */
	public static Result removeBudget(long id) {
		String username = session("connected_username");
		String userId = session().get("connected_id");
		if (username != null) {
			if (Budget.isOwner(Long.parseLong(userId), id)) {
				Budget.remove(id);
				return budgets();
			}
			return redirect(routes.Application.index());
		}
		return redirect(routes.Application.index());

	}
	
}
