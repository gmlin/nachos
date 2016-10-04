package nachos.kernel.threads.test;

import java.util.Random;

import nachos.kernel.Nachos;
import nachos.kernel.threads.Callout;
import nachos.kernel.threads.Condition;
import nachos.kernel.threads.Lock;
import nachos.machine.NachosThread;
import nachos.machine.Timer;

public class CalloutTest {
    
    private static final int NUM_THREADS = 16;
    
    public static void start() {
	Callout callout = Callout.getInstance();
	Random random = new Random();
	Lock lock = new Lock("Lock");
	Condition condition = new Condition("Threads finished", lock);
	
	for (int i = 0; i < NUM_THREADS; i++) {
	    NachosThread thread = new NachosThread("Callout" + i, new Runnable() {
		@Override
		public void run() {
		    int ticks = random.nextInt(10000);
		    Nachos.scheduler.sleepThread(ticks);
		    
		    lock.acquire();
		    callout.incrementCalloutsPerformed();
		    condition.signal();
		    lock.release();
		    
		    Nachos.scheduler.finishThread();
		}
	    });
	    Nachos.scheduler.readyToRun(thread);
	}
	
	lock.acquire();
	while (callout.getCalloutsPerformed() < NUM_THREADS) { // check if all threads are finished
	    condition.await();
	}
	lock.release();
	
	Timer timer = callout.getTimer();
	timer.stop();
    }
}
