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
    private static <T> void runOfferThread(String name, SynchronousQueue<T> queue, T obj) {
	NachosThread thread = new NachosThread(name, new Runnable() {
	    @Override
	    public void run() {
		queue.offer(obj);
	    	Nachos.scheduler.finishThread();
	    }
	});
	Nachos.scheduler.readyToRun(thread);
    }
    
    private static <T> void runPollThread(String name, SynchronousQueue<T> queue) {
	NachosThread thread = new NachosThread(name, new Runnable() {
	    @Override
	    public void run() {
		queue.poll();
	    	Nachos.scheduler.finishThread();
	    }
	});
	Nachos.scheduler.readyToRun(thread);
    }
    
    public static void start() {
	SynchronousQueue<Integer> queue = new SynchronousQueue<>();
	
	runTakeThread("Thread 1", queue);
	runTakeThread("Thread 2", queue);
	runPutThread("Thread 3", queue, 1);
	runPutThread("Thread 4", queue, 2);
	runTakeThread("Thread 5", queue);
	runPutThread("Thread 6", queue, 3);
	runTakeThread("Thread 7", queue);
	runPutThread("Thread 8", queue, 4);
	runPutThread("Thread 9", queue, 5);
	runPutThread("Thread 10", queue, 6);
	runTakeThread("Thread 11", queue);
	runTakeThread("Thread 12", queue);
	
//	runTakeThread("Thread 1", queue);
//	runTakeThread("Thread 2", queue);
//	runOfferThread("Thread 3", queue, 100);
//	runOfferThread("Thread 4", queue, 200);
	
//	runPutThread("Thread 3", queue, 100);
//	runPutThread("Thread 4", queue, 200);
//	runPollThread("Thread 1", queue);
//	runPollThread("Thread 2", queue);
    }
    
}
