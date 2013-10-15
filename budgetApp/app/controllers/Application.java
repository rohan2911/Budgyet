package controllers;

import play.mvc.*;

import views.html.*;

public class Application extends Controller {
	
    public static Result index() {
    	String username = session("connected");
    	if (username != null) {
    		return ok(bonus.render());
    	} else {
    		return ok(index.render());
    	}
    }
  
}
