package nachos.kernel.threads.test;

import java.util.Random;

import nachos.kernel.Nachos;
import nachos.machine.NachosThread;
import nachos.util.SynchronousQueue;

public class SynchronousQueueTest {

    private static <T> void runPutThread(String name, SynchronousQueue<T> queue, T obj) {
	NachosThread thread = new NachosThread(name, new Runnable() {
	    @Override
	    public void run() {
		queue.put(obj);
	    	Nachos.scheduler.finishThread();
	    }
	});
	Nachos.scheduler.readyToRun(thread);
    }
    
    private static <T> void runTakeThread(String name, SynchronousQueue<T> queue) {
	NachosThread thread = new NachosThread(name, new Runnable() {
	    @Override
	    public void run() {
		queue.take();
	    	Nachos.scheduler.finishThread();
	    }
	});
	Nachos.scheduler.readyToRun(thread);
    }
    
    public static void start() {
	SynchronousQueue<Integer> queue = new SynchronousQueue<>();
	
	Random random = new Random();
	
	runTakeThread("Thread 1", queue);
	runTakeThread("Thread 2", queue);
	runPutThread("Thread 3", queue, random.nextInt(1000));
	runPutThread("Thread 4", queue, random.nextInt(1000));
	runTakeThread("Thread 5", queue);
	runPutThread("Thread 6", queue, random.nextInt(1000));
	runTakeThread("Thread 7", queue);
	runPutThread("Thread 8", queue, random.nextInt(1000));
    }
    
}
