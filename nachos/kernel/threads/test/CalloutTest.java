package nachos.kernel.threads.test;

import java.util.Random;

import nachos.kernel.Nachos;
import nachos.kernel.threads.Callout;
import nachos.machine.NachosThread;
import nachos.machine.Timer;

public class CalloutTest {
    
    private static final int NUM_THREADS = 16;
    
    public static void start() {
	Callout callout = Callout.getInstance();
	Random random = new Random();
	
	for (int i = 0; i < NUM_THREADS; i++) {
	    NachosThread thread = new NachosThread("Callout" + i, new Runnable() {
		@Override
		public void run() {
		    int ticks = random.nextInt(1000);
		    Nachos.scheduler.sleepThread(ticks);
		    Nachos.scheduler.finishThread();
		}
	    });
	    Nachos.scheduler.readyToRun(thread);
	}
	
	Nachos.scheduler.sleepThread(1000);
	Timer timer = callout.getTimer();
	timer.stop();
    }
}
