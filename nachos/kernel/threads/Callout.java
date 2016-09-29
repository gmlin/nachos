package nachos.kernel.threads;

import java.util.ArrayList;
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
	timer.setHandler(new CalloutInterruptHandler(CalloutInitializer.INSTANCE));
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
    
    public static Callout getInstance() {
	return CalloutInitializer.INSTANCE;
    }
}
