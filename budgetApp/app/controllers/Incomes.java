package controllers;

import models.Income;
import models.ScheduledIncome;
import play.data.DynamicForm;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.*;

public class Incomes extends Controller {

	public static Result addIncome() {
		
		DynamicForm form = DynamicForm.form().bindFromRequest();
    	String amount = form.get("income_amount");
    	String tags = form.get("income_tag_list");
    	String date = form.get("income_date");
    	String description = form.get("income_description");
		
    	String repeat = form.get("income_repeat");
    	
		if (repeat != null && !repeat.equals("0")) {
			// create with scheduled repeat
	    	long id = 0;
			ScheduledIncome scheduledIncome = new ScheduledIncome(date, repeat, session().get("connected_id"), amount, description, tags);
			id = ScheduledIncome.add(scheduledIncome);
			
			ScheduledIncome.init(id);
		} else {
			// create only income
			Income income = new Income(session().get("connected_id"), amount, tags, date, description);
			Income.add(income);
		}
		
		
		return redirect(routes.Application.index());
	}
	
	public static Result incomes() {
    	return ok(income.render(Income.getIncomes(session("connected_id"))));
	}

}

