package nachos.kernel.userprog;

import nachos.Debug;
import nachos.kernel.Nachos;
import nachos.machine.CPU;
import nachos.machine.InterruptHandler;
import nachos.machine.Machine;
import nachos.machine.NachosThread;
import nachos.machine.Timer;
import nachos.util.FIFOQueue;
import nachos.util.Queue;

public class RoundRobin implements ReadyList {

    private final int QUANTUM = 1000;
    private final Queue<NachosThread> queue;
    private int timeElapsed;
    
    public RoundRobin() {
	for(int i = 0; i < Machine.NUM_CPUS; i++) {
	    CPU cpu = Machine.getCPU(i);
	    Timer timer = cpu.timer;
	    timer.setHandler(new RoundRobinInterruptHandler(timer));
	    timer.start();
	}
	
	queue = new FIFOQueue<>();
	timeElapsed = 0;
    }
    
    @Override
    public boolean offer(NachosThread e) {
	return queue.offer(e);
    }

    @Override
    public NachosThread peek() {
	return queue.peek();
    }

    @Override
    public NachosThread poll() {
	timeElapsed = 0;
	return queue.poll();
    }

    @Override
    public boolean isEmpty() {
	return queue.isEmpty();
    }

    class RoundRobinInterruptHandler implements InterruptHandler {

	private final Timer timer;
	
	public RoundRobinInterruptHandler(Timer timer) {
	    this.timer = timer;
	}
	
	@Override
	public void handleInterrupt() {
	    timeElapsed += timer.interval;
	    if (timeElapsed >= QUANTUM) {
		Debug.println('0', "Quantum exceeded, yielding");
		timeElapsed = 0;
		yieldOnReturn();
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
