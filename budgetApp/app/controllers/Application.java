package controllers;


import play.data.*;
import play.mvc.*;
import models.*;

import views.html.*;

public class Application extends Controller {
  
	static Form<Task> taskForm = Form.form(Task.class);
	
    public static Result index() {
        return redirect(routes.Application.tasks());
    }
    
    public static Result tasks() {
    	return ok(index.render(Task.all(), taskForm));
    }
    
    public static Result newTask() {
    	Form<Task> filledForm = taskForm.bindFromRequest();
    	if (filledForm.hasErrors()) {
    		return badRequest(index.render(Task.all(), filledForm));
    	} else {
    		Task.create(filledForm.get());
    		return redirect(routes.Application.tasks());
    	}
    }
    
    public static Result deleteTask(Long id) {
    	Task.delete(id);
    	return redirect(routes.Application.tasks());
    }
    
    public static Result login() {
    	final DynamicForm form = DynamicForm.form().bindFromRequest();
    	final String username = form.get("username");
    	final String password = form.get("password");
    	
    	if (username.equals("rohan") && password.equals("password")) {
    		return ok(bonus.render());
    	} else {
    		
    	}
    	
    	return ok();
    }
  
}
