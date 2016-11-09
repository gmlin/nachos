package nachos.kernel.userprog;

import java.util.ArrayList;
import java.util.List;

import nachos.Debug;
import nachos.kernel.Nachos;
import nachos.machine.CPU;
import nachos.machine.InterruptHandler;
import nachos.machine.Machine;
import nachos.machine.NachosThread;
import nachos.machine.Timer;
import nachos.util.FIFOQueue;
import nachos.util.Queue;

public class FeedbackScheduling implements ReadyList {

    private final int QUANTUM = 1000;
    private final List<Queue<UserThread>> queues;
    private final int NUM_QUEUES = 5;
    private int timeElapsed;
    
    public FeedbackScheduling() {
	for(int i = 0; i < Machine.NUM_CPUS; i++) {
	    CPU cpu = Machine.getCPU(i);
	    Timer timer = cpu.timer;
	    timer.setHandler(new FeedbackSchedulingInterruptHandler(timer));
	    timer.start();
	}
	
	queues = new ArrayList<>();
	
	for (int i = 0; i < NUM_QUEUES; i++) {
	    queues.add(new FIFOQueue<UserThread>());
	}
	
	timeElapsed = 0;
    }
    
    @Override
    public boolean offer(UserThread e) {
	boolean added = queues.get(e.nextQueue).offer(e);
	if (e.nextQueue + 1 < NUM_QUEUES)
	    e.nextQueue++;
	
	return added;
    }

    @Override
    public UserThread peek() {
	for (int i = 0; i < NUM_QUEUES; i++) {
	    if (!queues.get(i).isEmpty())
		return queues.get(i).peek();
	}
	return null;
    }

    @Override
    public UserThread poll() {
	timeElapsed = 0;
	for (int i = 0; i < NUM_QUEUES; i++) {
	    if (!queues.get(i).isEmpty())
		return queues.get(i).poll();
	}
	return null;
    }

    @Override
    public boolean isEmpty() {
	for (int i = 0; i < NUM_QUEUES; i++) {
	    if (!queues.get(i).isEmpty())
		return false;
	}
	return true;
    }

    class FeedbackSchedulingInterruptHandler implements InterruptHandler {

	private final Timer timer;
	
	public FeedbackSchedulingInterruptHandler(Timer timer) {
	    this.timer = timer;
	}
	
	@Override
	public void handleInterrupt() {
	    UserThread thread = (UserThread)NachosThread.currentThread();
	    if (thread != null) {
		timeElapsed += timer.interval;
		    if (timeElapsed >= QUANTUM * thread.nextQueue) {
			Debug.println('0', "Quantum exceeded, yielding");
			timeElapsed = 0;
			yieldOnReturn();
		    }
	    }
	    
	}

	private void yieldOnReturn() {
	    CPU.setOnInterruptReturn
	    (new Runnable() {
		public void run() {
		    if (NachosThread.currentThread() != null) {
			Debug.println('t', "Yielding current thread on interrupt return");
			Nachos.scheduler.yieldThread();
		    } else {
			Debug.println('i', "No current thread on interrupt return, skipping yield");
		    }
		}
	    });
	}
	
    }
}
