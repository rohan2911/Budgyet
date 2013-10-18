import play.*;
import play.mvc.Action;
import play.mvc.Http.Request;
import java.lang.reflect.Method;

public class Global extends GlobalSettings {

	public void onStart(Application app) {
        Logger.info("Application has started");
    }

    public void onStop(Application app) {
        Logger.info("Application shutdown...");
    }
    
    public Action onRequest(Request request, Method actionMethod) {
        System.out.println("before each request..." + request.toString());
        return super.onRequest(request, actionMethod);
    }
	
	
}
