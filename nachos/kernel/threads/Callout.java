package nachos.kernel.threads;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import nachos.machine.Machine;
import nachos.machine.Timer;

public class Callout {

    private final List<ScheduledRunnable> runnables;
    private final Timer timer;
    private long currentTime;
    
    /** Nested static class to ensure only on Callout object is created */
    private static class CalloutInitializer {
	private static final Callout INSTANCE = new Callout();
    }
    
    private Callout() {
	runnables = new ArrayList<>();
	timer = Machine.getTimer(0);
	timer.setHandler(new CalloutInterruptHandler(timer, CalloutInitializer.INSTANCE));
	currentTime = 0;
    }
    
    /**
     * Schedule a callout to occur at a specified number of
     * ticks in the future.
     *
     * @param runnable  A Runnable to be invoked when the specified
     * time arrives.
     * @param ticksFromNow  The number of ticks in the future at
     * which the callout is to occur.
     */
    public void schedule(final Runnable runnable, final int ticksFromNow) {
	runnables.add(new ScheduledRunnable(runnable, currentTime + ticksFromNow));
    }
    
    public void updateCurrentTime(long timeElapsed) {
	currentTime += timeElapsed;
    }
    
    public void performCallouts() {
	Collections.sort(runnables, ScheduledRunnableComparator.getInstance());
	Iterator<ScheduledRunnable> iterator = runnables.iterator();
	
	while (iterator.hasNext()) {
	    ScheduledRunnable runnable = iterator.next();
	    if (runnable.isReadyToRun(currentTime)) {
		iterator.remove();
		runnable.run();
	    }
	    else
		break;
	}
    }
    
    public static Callout getInstance() {
	return CalloutInitializer.INSTANCE;
    }
}
