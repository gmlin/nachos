package nachos.kernel.threads;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import nachos.machine.CPU;
import nachos.machine.Machine;
import nachos.machine.Timer;

public class Callout {

    private List<ScheduledRunnable> runnables;
    private Timer timer;
    private long currentTime;
    
    /** Nested static class to ensure only on Callout object is created */
    private static class CalloutInitializer {
	private static final Callout INSTANCE = new Callout();
    }
    
    private Callout() {
	runnables = new ArrayList<>();
	timer = Machine.getTimer(0);
	timer.setHandler(new CalloutInterruptHandler());
	currentTime = 0;
	timer.start();
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
    public void schedule(Runnable runnable, int ticksFromNow) {
	int oldLevel = CPU.setLevel(CPU.IntOff);
	runnables.add(new ScheduledRunnable(runnable, currentTime + ticksFromNow));
	CPU.setLevel(oldLevel);
    }
    
    public void updateCurrentTime() {
	currentTime += timer.interval;
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
    
    public Timer getTimer() {
	return timer;
    }
    
    public static Callout getInstance() {
	return CalloutInitializer.INSTANCE;
    }
}
