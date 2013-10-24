import play.*;
import play.libs.Akka;
import play.mvc.Action;
import play.mvc.Http.Request;

import java.lang.reflect.Method;

import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

import controllers.ScheduleActor;

public class Global extends GlobalSettings {

 public void onStart(Application app) {
        Logger.info("Application has started");
        
        ActorRef scheduleRef = Akka.system().actorOf(Props.create(ScheduleActor.class));
        
        Akka.system().scheduler().schedule(Duration.Zero(),
        		Duration.create(5, TimeUnit.SECONDS),
        		scheduleRef,
        		"Scheduled Tasks",
        		Akka.system().dispatcher(), null);
    }

    public void onStop(Application app) {
        Logger.info("Application shutdown...");
    }
    
    public Action onRequest(Request request, Method actionMethod) {
        System.out.println("before each request..." + request.toString());
        return super.onRequest(request, actionMethod);
    }


}