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
    	String description = form.get("description");
		
		/*System.out.println("tags = " + tags);
		System.out.println("tagslength = " + tags.length());
		System.out.println("date = " + date);*/
		
		Income income = new Income(amount, tags, date, description);
		System.out.println("tag isempty = " + income.tags.isEmpty());
	
		return redirect(routes.Application.index());
	}
	
}
