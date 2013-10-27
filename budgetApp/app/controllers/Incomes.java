package controllers;

import java.math.BigDecimal;
import java.util.List;

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
//			scheduledIncome.period = Integer.parseInt(repeat);
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
		List<String> tagNames = Income.getTags(session().get("connected_id"));	// get list of tag names
		List<String> tagSums = Income.getTagSum(session().get("connected_id"));	// get sum of the income values by tag
		
		
		// return order: list of all incomes as Incomes object, string of tag names, string of the sum of income values by tag.
    	return ok(incomes.render(Income.getIncomes(session("connected_id")), Income.listToString(tagNames), 
    			Income.listToString(tagSums), tagNames));
	}
	
	// TODO: this method
	public static Result showEditIncome(long id) {
		List<String> tagNames = Income.getTags(session().get("connected_id"));	// get list of tag names
		Income inc = Income.getById(Long.parseLong(session().get("connected_id")), id);
		return ok(editIncome.render(inc, tagNames));
	}
	
	// TODO: this method
	public static Result editIncome(long id) {
		return ok();
	}

}

