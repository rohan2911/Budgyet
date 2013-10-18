package controllers;

import play.mvc.*;

import views.html.*;

public class Application extends Controller {
	
    public static Result index() {
    	String username = session("connected_username");
    	if (username != null) {
    		return ok(home.render());
    	} else {
    		return ok(index.render());
    	}
    }
  
}
