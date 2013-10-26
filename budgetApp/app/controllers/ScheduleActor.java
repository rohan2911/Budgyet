package controllers;

import akka.actor.UntypedActor;
import models.ScheduledIncome;
import models.ScheduledExpense;

/**

* Actor for running scheduled tasks

*/

public class ScheduleActor extends UntypedActor {

	@Override
	public void onReceive(Object arg0) throws Exception {
		ScheduledIncome.scheduledTask();
		ScheduledExpense.scheduledTask();
	}
}