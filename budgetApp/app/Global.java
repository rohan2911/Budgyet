import play.*;
import play.libs.Akka;
import play.mvc.Action;
import play.mvc.Http.Request;

import java.lang.reflect.Method;

import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public class Global extends GlobalSettings {

	public void onStart(Application app) {
        Logger.info("Application has started");
        
        /*Akka.system().scheduler().schedule(Duration.create(0, TimeUnit.MILLISECONDS),
        		Duration.create(5, TimeUnit.SECONDS),
        		  	new Runnable() {
			            @Override
			            public void run() {
			              	System.out.println("running the actor");
			            }
        			}, Akka.system().dispatcher());*/

    }

    public void onStop(Application app) {
        Logger.info("Application shutdown...");
    }
    
    public Action onRequest(Request request, Method actionMethod) {
        System.out.println("before each request..." + request.toString());
        return super.onRequest(request, actionMethod);
    }
	
}
