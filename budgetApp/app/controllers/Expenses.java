package controllers;

import models.Expense;
import play.data.DynamicForm;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.*;

public class Expenses extends Controller {
	
	public static Result addExpense() {
		Form<Expense> ExpenseForm = Form.form(Expense.class);
		ExpenseForm = ExpenseForm.bindFromRequest();
		Expense.add(ExpenseForm.get());
		return redirect(routes.Application.index());
	}
	
}