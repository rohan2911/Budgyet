package controllers;

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
    	String description = form.get("description");
		
		System.out.println(tags);
		System.out.println(date);
		
		return redirect(routes.Application.index());
	}
	
}