package controllers;

import models.Expense;
import models.ScheduledExpense;
import play.data.DynamicForm;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.*;

public class Expenses extends Controller {
	
	public static Result addExpense() {
		
		DynamicForm form = DynamicForm.form().bindFromRequest();	// throws form params into this object
    	String amount = form.get("expense_amount");
    	String tags = form.get("expense_tag_list");
    	String date = form.get("expense_date");
    	String description = form.get("expense_description");
		
    	String repeat = form.get("expense_repeat");
    	
		if (repeat != null && !repeat.equals("0")) {
			// create with scheduled repeat
	    	long id = 0;
			ScheduledExpense scheduledExpense = new ScheduledExpense(date, repeat, session().get("connected_id"), amount, description, tags);
			id = ScheduledExpense.add(scheduledExpense);
			
			ScheduledExpense.init(id);
		} else {
			// create only income
			Expense expense = new Expense(session().get("connected_id"), amount, tags, date, description);
			Expense.add(expense);
		}
    	
		return redirect(routes.Application.index());
	}
	
	public static Result expenses() {
    	return ok(expenses.render());
	}
	
}