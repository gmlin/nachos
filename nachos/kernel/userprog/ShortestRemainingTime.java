package nachos.kernel.userprog;

import java.util.Comparator;
import java.util.PriorityQueue;

import nachos.Debug;
import nachos.kernel.Nachos;
import nachos.machine.CPU;
import nachos.machine.InterruptHandler;
import nachos.machine.Machine;
import nachos.machine.NachosThread;
import nachos.machine.Timer;
import nachos.util.FIFOQueue;
import nachos.util.Queue;

public class ShortestRemainingTime implements ReadyList {

    private final PriorityQueue<UserThread> queue;
    
    public ShortestRemainingTime() {
	for(int i = 0; i < Machine.NUM_CPUS; i++) {
	    CPU cpu = Machine.getCPU(i);
	    Timer timer = cpu.timer;
	    timer.setHandler(new ShortestRemainingTimeInterruptHandler(timer));
	    timer.start();
	}
	
	queue = new PriorityQueue<UserThread>(new Comparator<UserThread>() {

	    @Override
	    public int compare(UserThread o1, UserThread o2) {
		return o1.timeLeft - o2.timeLeft;
	    }
	    
	});
    }
    
    @Override
    public boolean offer(UserThread e) {
	return queue.offer(e);
    }

    @Override
    public UserThread peek() {
	return queue.peek();
    }

    @Override
    public UserThread poll() {
	return queue.poll();
    }

    @Override
    public boolean isEmpty() {
	return queue.isEmpty();
    }

    class ShortestRemainingTimeInterruptHandler implements InterruptHandler {

	private final Timer timer;
	
	public ShortestRemainingTimeInterruptHandler(Timer timer) {
	    this.timer = timer;
	}
	
	@Override
	public void handleInterrupt() {
	    yieldOnReturn();
	}

	private void yieldOnReturn() {
	    CPU.setOnInterruptReturn
	    (new Runnable() {
		public void run() {
		    UserThread thread = (UserThread)NachosThread.currentThread();
		    if (thread != null) {
			thread.timeLeft -= timer.interval;
			if (!queue.isEmpty() && queue.peek().timeLeft < thread.timeLeft) {
			    Debug.println('t', "Yielding current thread on interrupt return");
			    Nachos.scheduler.yieldThread();
			}
		    } else {
			Debug.println('i', "No current thread on interrupt return, skipping yield");
		    }
		}
	    });
	}
	
    }
}
