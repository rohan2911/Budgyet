package controllers;

import models.Expense;
import play.data.DynamicForm;
import play.mvc.Controller;
import play.mvc.Result;

public class Expenses extends Controller {
	
	public static Result addExpense() {
		
		DynamicForm form = DynamicForm.form().bindFromRequest();
    	String amount = form.get("expense_amount");
    	String tags = form.get("expense_tag_list");
    	String date = form.get("expense_date");
    	String description = form.get("expense_description");
		
    	String repeat = form.get("expense_repeat");
		
    	Expense expense = new Expense(session().get("connected_id"), amount, tags, date, description);
		Expense.add(expense);
    	
		return redirect(routes.Application.index());
	}
	
}