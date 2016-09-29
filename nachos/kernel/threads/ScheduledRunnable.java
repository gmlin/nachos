package nachos.kernel.threads;

public class ScheduledRunnable implements Runnable {
    
    private final Runnable runnable;
    private final long scheduledTime;
    
    public ScheduledRunnable(final Runnable runnable, final long scheduledTime) {
        this.runnable = runnable;
        this.scheduledTime = scheduledTime;
    }
    
    @Override
    public void run() {
        runnable.run();	    
    }
    
    public boolean isReadyToRun(final long currentTime) {
        return currentTime >= scheduledTime;
    }
    
    public long getScheduledTime() {
	return scheduledTime;
    }
}