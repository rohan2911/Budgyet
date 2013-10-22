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
        
        Akka.system().scheduler().schedule(Duration.create(0, TimeUnit.MILLISECONDS),
          Duration.create(5, TimeUnit.SECONDS),
             new Runnable() {
               @Override
               public void run() {
                  System.out.println("running the actor");
               }
           }, Akka.system().dispatcher());

    }

    public void onStop(Application app) {
        Logger.info("Application shutdown...");
    }
    
    public Action onRequest(Request request, Method actionMethod) {
        System.out.println("before each request..." + request.toString());
        return super.onRequest(request, actionMethod);
    }
 
}

/*import play.*;
import play.mvc.Action;
import play.mvc.Http.Request;

import java.lang.reflect.Method;

import play.libs.Akka;
import play.libs.F.Promise;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.util.duration;


public class Global extends GlobalSettings {

	public void onStart(Application app) {
        Logger.info("Application has started");
        
        ActorRef scheduleRef = Akka.system().actorOf(Props.create(scheduleActor.class));
        
        Akka.system().scheduler().schedule(
        		  Duration.create(0, TimeUnit.MILLISECONDS),
        		  Duration.create(30, TimeUnit.MINUTES),
        		  scheduleRef, 
        		  "tick"
        );
        
    }

    public void onStop(Application app) {
        Logger.info("Application shutdown...");
    }
    
    public Action onRequest(Request request, Method actionMethod) {
        System.out.println("before each request..." + request.toString());
        return super.onRequest(request, actionMethod);
    }
    
    *//**
    *
    * Actor for running scheduled tasks
    *
    *//*
    
    static class scheduleActor extends UntypedActor {

		@Override
		public void onReceive(Object arg0) throws Exception {
			// TODO Auto-generated method stub
			
		}
    	
    }
    
    
    public static class Scheduled implements Callable <String> {
    	
    	public List<String> scheduledModels;
    	
    	public Scheduled () {
    		scheduledModels = new ArrayList <String> ();
    	}
    	
		@Override
		public String call() throws Exception {
			System.out.println("DICKSDICKSDICKS");
			return null;
		}
		
    }
	
	
}*/
