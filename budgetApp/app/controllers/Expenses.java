package controllers;

import java.math.BigDecimal;
import java.util.List;

import models.Expense;
import models.ScheduledExpense;
import play.data.DynamicForm;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.*;

public class Expenses extends Controller {

	public static Result addExpense() {
		
		DynamicForm form = DynamicForm.form().bindFromRequest();
    	String amount = form.get("expense_amount");
    	String tags = form.get("expense_tag_list");
    	String date = form.get("expense_date");
    	String description = form.get("expense_description");
    	String source = form.get("source");
		
    	String repeat = form.get("expense_repeat");
    	
		if (repeat != null && !repeat.equals("0")) {
			// create with scheduled repeat
	    	long id = 0;
			ScheduledExpense scheduledExpense = new ScheduledExpense(date, repeat, session().get("connected_id"), amount, description, tags);
			id = ScheduledExpense.add(scheduledExpense);
			
			ScheduledExpense.init(id);
		} else {
			// create only expense
			Expense expense = new Expense(session().get("connected_id"), amount, tags, date, description);
			Expense.add(expense);
		}
		
		if (source.equals("expenses")) {
			return redirect(routes.Expenses.expenses());
		} else {
			return redirect(routes.Application.index());
		}
	}
	
	public static Result expenses() {
		List<String> tagNames = Expense.getTags(session().get("connected_id"));	// get list of tag names
		List<String> expenseTagNames = Expense.getExpenseTags(session().get("connected_id"));	// get list of expense tag names for graph
		List<String> tagSums = Expense.getTagSum(session().get("connected_id"));	// get sum of the expense values by tag
		
		if (expenseTagNames.size() == 0) {
			expenseTagNames.add("Nothing");
			tagSums.add("0.00");
		}
		
		// return order: list of all expenses as Expenses object, string of tag names, string of the sum of expense values by tag.
    	return ok(expenses.render(Expense.getExpenses(session("connected_id")), Expense.listToString(tagNames), Expense.listToString(expenseTagNames),
    			Expense.listToString(tagSums), tagNames));
	}
	
	public static Result showEditExpense(long id) {
		List<String> tagNames = Expense.getTags(session().get("connected_id"));	// get list of tag names
		Expense inc = Expense.get(id);
		ScheduledExpense schinc = ScheduledExpense.get(id);
		
		return ok(editExpense.render(inc, schinc, tagNames));
	}
	
	public static Result editExpense(long id) {
		DynamicForm form = DynamicForm.form().bindFromRequest();
		
    	String amount = form.get("expense_amount");
    	String tag = form.get("expense_tag_list");
    	String date = form.get("expense_date");
    	String description = form.get("expense_description");
    	
    	String repeat = form.get("expense_repeat");
    	Expense expense = new Expense(session().get("connected_id"), amount, tag, date, description);
    	
    	expense.id = id;
    	
    	if (expense.getScheduler()) {
    		// get the way we want to apply the scheduler edit
    		int applyMode = Integer.parseInt(form.get("scheduled_apply"));
    		
    		if (applyMode == 1) {
    			// apply to all expenses after the date
    			
    			ScheduledExpense scheduledExpense = new ScheduledExpense(date, repeat, session().get("connected_id"), amount, description, tag);
    			
    			scheduledExpense.update(expense.scheduler);
    		} else {
    			if (repeat != null && !repeat.equals("0")) {
    				// create with scheduled repeat
    		    	long scheduledExpenseId = 0;
    				ScheduledExpense scheduledExpense = new ScheduledExpense(date, repeat, session().get("connected_id"), amount, description, tag);
    				scheduledExpenseId = ScheduledExpense.add(scheduledExpense);
    				
    				// remove the expense in the db, it will be recreated by init
    				Expense.remove(id);
    				ScheduledExpense.init(id);
    			} else if (repeat.equals("0")) {
    				// stop a payment recurring
    				ScheduledExpense.remove(expense.scheduler);
    				expense.update(id);
    			} else {
    				expense.update(id);
    			}
    		}
    		
    	} else {
    		expense.update(id);
    	}
    	return redirect(routes.Expenses.expenses());
	}
	
	public static Result removeExpense (long id) {
		// check if expense is repeating
		Expense expense = new Expense("0", "0.00", null, "0000-00-00", null);
		expense.id = id;
		boolean scheduled = expense.getScheduler();
		// remove expense
		Expense.remove(id);
		
		// remove scheduler if this is the last expense tied to it
		if (scheduled) { 
			ScheduledExpense.clean(expense.scheduler);
			System.out.println(expense.scheduler);
		}
		return redirect(routes.Expenses.expenses());
	}

}

