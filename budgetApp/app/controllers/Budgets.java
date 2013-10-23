package controllers;

import play.data.DynamicForm;
import play.mvc.Controller;
import play.mvc.Result;

public class Budgets extends Controller {

	public static Result addBudget() {
		
		DynamicForm form = DynamicForm.form().bindFromRequest();
		
		return redirect(routes.Application.index());
	}
	
}
