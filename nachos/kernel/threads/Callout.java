package nachos.kernel.threads;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import nachos.machine.CPU;
import nachos.machine.Machine;
import nachos.machine.Timer;

public class Callout {

    private List<ScheduledRunnable> runnables;
    private Timer timer;
    private long currentTime;
    private int calloutsPerformed;
    private SpinLock spinlock;
    
    /** Nested static class to ensure only on Callout object is created */
    private static class CalloutInitializer {
	private static final Callout INSTANCE = new Callout();
    }
    
    private Callout() {
	runnables = new ArrayList<>();
	timer = Machine.getTimer(0);
	timer.setHandler(new CalloutInterruptHandler());
	currentTime = 0;
	calloutsPerformed = 0;
	spinlock = new SpinLock("spinlock");
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
	ScheduledRunnable scheduledRunnable = new ScheduledRunnable(runnable, currentTime + ticksFromNow);
	spinlock.acquire();
	
	int insertIndex = Collections.binarySearch(runnables, scheduledRunnable, ScheduledRunnableComparator.getInstance());
	if (insertIndex < 0) {
	    insertIndex = -(insertIndex + 1);
	}
	
	runnables.add(insertIndex, scheduledRunnable);
	
	spinlock.release();
	CPU.setLevel(oldLevel);
    }
    
    public void updateCurrentTime() {
	currentTime += timer.interval;
    }
    
    public void performCallouts() {
	List<ScheduledRunnable> readyToBeRun = new ArrayList<>();
	
	spinlock.acquire();
	
	updateCurrentTime();
	ListIterator<ScheduledRunnable> iterator = runnables.listIterator(runnables.size());
	
	while (iterator.hasPrevious()) {
	    ScheduledRunnable runnable = iterator.previous();
	    if (runnable.isReadyToRun(currentTime)) {
		iterator.remove();
		readyToBeRun.add(runnable);
	    }
	    else
		break;
	}
	
	spinlock.release();
	
	for (ScheduledRunnable runnable : readyToBeRun) {
	    runnable.run();
	}
    }
    
    public void incrementCalloutsPerformed() {
	calloutsPerformed++;
    }
    
    public int getCalloutsPerformed() {
	return calloutsPerformed;
    }
    
    public Timer getTimer() {
	return timer;
    }
    
    public static Callout getInstance() {
	return CalloutInitializer.INSTANCE;
    }
}
