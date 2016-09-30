package nachos.kernel.threads;

public class ScheduledRunnable implements Runnable {
    
    private Runnable runnable;
    private long scheduledTime;
    
    public ScheduledRunnable(Runnable runnable, long scheduledTime) {
        this.runnable = runnable;
        this.scheduledTime = scheduledTime;
    }
    
    @Override
    public void run() {
        runnable.run();	    
    }
    
    public boolean isReadyToRun(long currentTime) {
        return currentTime >= scheduledTime;
    }
    
    public long getScheduledTime() {
	return scheduledTime;
    }
}