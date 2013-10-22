package controllers;


import models.Income;
import play.data.DynamicForm;
import play.mvc.Controller;
import play.mvc.Result;

public class Incomes extends Controller {

	public static Result addIncome() {
		
		DynamicForm form = DynamicForm.form().bindFromRequest();
    	String amount = form.get("income_amount");
    	String tags = form.get("income_tag_list");
    	String date = form.get("income_date");
    	String description = form.get("income_description");
		
    	String repeat = form.get("income_repeat");
    	
		Income income = new Income(session().get("connected_id"), amount, tags, date, description);
		Income.add(income);
	
		return redirect(routes.Application.index());
	}
}

