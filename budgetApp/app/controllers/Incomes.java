package controllers;

import java.math.BigDecimal;
import java.util.List;

import models.Income;
import models.ScheduledIncome;
import play.data.DynamicForm;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.*;

/**
 * Controls the incomes.
 * @author Rohan, Leslie, Tracey, Jeremy, Hana
 */
public class Incomes extends Controller {

	/**
	 * Adds the income to the system. 
	 * @return if user is logged in, redirect to the incomes page, 
	 * 			otherwise, redirect to the index page.
	 */
	public static Result addIncome() {
		String username = session("connected_username");
		if (username != null) {
			
			DynamicForm form = DynamicForm.form().bindFromRequest();
	    	String amount = form.get("income_amount");
	    	String tags = form.get("income_tag_list");
	    	String date = form.get("income_date");
	    	String description = form.get("income_description");
	    	String source = form.get("source");
			
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
			
			
			if (source.equals("incomes")) {
				return redirect(routes.Incomes.incomes());
			} else {
				return redirect(routes.Application.index());
			}
		}
		return redirect(routes.Application.index());
		
	}
	
	/**
	 * Displays the incomes page. 
	 * @return if user is logged in, load the incomes page,
	 * 			otherwise, redirect to the index page.
	 */
	public static Result incomes() {
		
		String username = session("connected_username");
    	if (username == null) {
    		return redirect(routes.Application.index());
    	}
		
		List<String> tagNames = Income.getTags(session().get("connected_id"));	// get list of tag names
		List<String> incomeTagNames = Income.getIncomeTags(session().get("connected_id"));	// get list of income tag names for graph
		List<String> tagSums = Income.getTagSum(session().get("connected_id"));	// get sum of the income values by tag
		
		if (incomeTagNames.size() == 0) {
			incomeTagNames.add("Nothing");
			tagSums.add("0.00");
		}
		
		// return order: list of all incomes as Incomes object, string of tag names, string of the sum of income values by tag.
    	return ok(incomes.render(Income.getIncomes(session("connected_id")), Income.listToString(tagNames), Income.listToString(incomeTagNames),
    			Income.listToString(tagSums), tagNames));
	}
	
	/**
	 * Load the edit page for the specified income. 
	 * @param id internal id of the income to be edited
	 * @return load the edit income page if user is logged in,
	 * 			otherwise redirect to the index page.
	 */
	public static Result showEditIncome(long id) {
		
		String username = session("connected_username");
		String userId = session().get("connected_id");
		if (username != null) {
			List<String> tagNames = Income.getTags(userId);	// get list of tag names
			Income inc = Income.get(id);
			ScheduledIncome schinc = ScheduledIncome.get(id);
			
	    	if (inc == null || Long.parseLong(userId) != inc.owner) {
	    		return redirect(routes.Application.index());
	    	}
			
			return ok(editIncome.render(inc, schinc, tagNames));
		}
		
		return redirect(routes.Application.index());
		
		
	}
	
	/**
	 * Edits the income on the system.
	 * @param id internal id of the income to be edited
	 * @return if user is logged in, redirect to the incomes page,
	 * 			otherwise, redirect to the index page.
	 */
	public static Result editIncome(long id) {
		String username = session("connected_username");
		if (username != null) {
			DynamicForm form = DynamicForm.form().bindFromRequest();
			
	    	String amount = form.get("income_amount");
	    	String tag = form.get("income_tag_list");
	    	String date = form.get("income_date");
	    	String description = form.get("income_description");
	    	
	    	String repeat = form.get("income_repeat");
	    	Income income = new Income(session().get("connected_id"), amount, tag, date, description);
	    	
	    	income.id = id;
	    	
	    	if (income.getScheduler()) {
	    		// get the way we want to apply the scheduler edit
	    		int applyMode = Integer.parseInt(form.get("scheduled_apply"));
	    		
	    		if (applyMode == 1) {
	    			// apply to all incomes after the date
	    			
	    			ScheduledIncome scheduledIncome = new ScheduledIncome(date, repeat, session().get("connected_id"), amount, description, tag);
	    			
	    			scheduledIncome.update(income.scheduler);
	    		} else {
	    			if (repeat != null && !repeat.equals("0")) {
	    				// create with scheduled repeat
	    		    	long scheduledIncomeId = 0;
	    				ScheduledIncome scheduledIncome = new ScheduledIncome(date, repeat, session().get("connected_id"), amount, description, tag);
	    				scheduledIncomeId = ScheduledIncome.add(scheduledIncome);
	    				
	    				// remove the income in the db, it will be recreated by init
	    				Income.remove(id);
	    				ScheduledIncome.init(id);
	    			} else if (repeat.equals("0")) {
	    				// stop a payment recurring
	    				ScheduledIncome.remove(income.scheduler);
	    				income.update(id);
	    			} else {
	    				income.update(id);
	    			}
	    		}
	    		
	    	} else {
	    		income.update(id);
	    	}
	    	return redirect(routes.Incomes.incomes());
		}
		return redirect(routes.Application.index());
	}
	
	/**
	 * Removes the income from the system.
	 * @param id internal id of the income to be removed from the db.
	 * @return if user is logged in, redirect to the incomes page,
	 * 			otherwise, redirect to the index page.
	 */
	public static Result removeIncome (long id) {
		String username = session("connected_username");
		String userId = session().get("connected_id");
		if (username != null) {
			Income inc = Income.get(id);
			if (inc == null || Long.parseLong(userId) != inc.owner) {
				return redirect(routes.Application.index());
			}
			// check if income is repeating
			Income income = new Income("0", "0.00", null, "0000-00-00", null);
			income.id = id;
			boolean scheduled = income.getScheduler();
			// remove income
			Income.remove(id);
			
			// remove scheduler if this is the last income tied to it
			if (scheduled) { 
				ScheduledIncome.clean(income.scheduler);
				System.out.println(income.scheduler);
			}
			return redirect(routes.Incomes.incomes());
		}
		
		return redirect(routes.Application.index());
		
	}

}

