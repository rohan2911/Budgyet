import play.*;
import play.libs.Akka;
import play.mvc.Action;
import play.mvc.Http.Request;

import java.lang.reflect.Method;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import models.ScheduledIncome;
import akka.actor.ActorRef;
import akka.actor.Props;
import controllers.ScheduleActor;

public class Global extends GlobalSettings {

	/**
	 * Always executes when app starts.
	 * Also initialises the scheduler when the app starts up.
	 */
	public void onStart(Application app) {
        Logger.info("Application has started");
        
        ActorRef scheduleRef = Akka.system().actorOf(Props.create(ScheduleActor.class));
        
        ScheduledIncome.scheduledTask();
        
        // calculate the number of hours and minutes till 1 AM , and set to initial delay for scheduled tasks
        Calendar currDate = Calendar.getInstance();
        int timeHours = currDate.get(Calendar.HOUR_OF_DAY);
        int timeMinutes = currDate.get(Calendar.MINUTE);
        
        FiniteDuration initDelay = null;
        
        if (timeHours == 0) {
        	initDelay = Duration.create(60 - timeMinutes, TimeUnit.MINUTES);
        } else {
        	initDelay = Duration.create((25 - timeHours)*60 - timeMinutes, TimeUnit.MINUTES); 
        }
        
        Akka.system().scheduler().schedule(initDelay,
        		Duration.create(1, TimeUnit.DAYS),
        		scheduleRef,
        		"Scheduled Tasks",
        		Akka.system().dispatcher(), null);
    }

	/**
	 * Always called when the app is shutting down.
	 */
    public void onStop(Application app) {
        Logger.info("Application shutdown...");
    }
    
    /**
     * Called before each request.
     */
    public Action onRequest(Request request, Method actionMethod) {
        System.out.println("before each request..." + request.toString());
        return super.onRequest(request, actionMethod);
    }


}
