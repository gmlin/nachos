package nachos.util;

import nachos.Debug;
import nachos.kernel.Nachos;
import nachos.kernel.threads.Condition;
import nachos.kernel.threads.Lock;
import nachos.machine.NachosThread;

/**
 * This class is patterned after the SynchronousQueue class
 * in the java.util.concurrent package.
 *
 * A SynchronousQueue has no capacity: each insert operation
 * must wait for a corresponding remove operation by another
 * thread, and vice versa.  A thread trying to insert an object
 * enters a queue with other such threads, where it waits to
 * be matched up with a thread trying to remove an object.
 * Similarly, a thread trying to remove an object enters a
 * queue with other such threads, where it waits to be matched
 * up with a thread trying to insert an object.
 * If there is at least one thread waiting to insert and one
 * waiting to remove, the first thread in the insertion queue
 * is matched up with the first thread in the removal queue
 * and both threads are allowed to proceed, after transferring
 * the object being inserted to the thread trying to remove it.
 * At any given time, the <EM>head</EM> of the queue is the
 * object that the first thread on the insertion queue is trying
 * to insert, if there is any such thread, otherwise the head of
 * the queue is null.
 */

public class SynchronousQueue<T> implements Queue<T> {

    private Queue<T> queue;
    private Lock lock;
    private Lock putLock;
    private Lock takeLock;
    private Condition itemTaken;
    private Condition itemAdded;
    private int awaitingTake;
    private int awaitingPut;
    private T lastRemoved;
    
    /**
     * Initialize a new SynchronousQueue object.
     */
    public SynchronousQueue() {
	queue = new FIFOQueue<T>();
	lock = new Lock("Queue lock");
	putLock = new Lock("Put lock");
	takeLock = new Lock("Take lock");
	itemTaken = new Condition("Item taken", lock);
	itemAdded = new Condition("Item added", lock);
	awaitingTake = 0;
	awaitingPut = 0;
	lastRemoved = null;
    }

    /**
     * Adds the specified object to this queue,
     * waiting if necessary for another thread to remove it.
     *
     * @param obj The object to add.
     */
    public boolean put(T obj) {
	NachosThread currentThread = NachosThread.currentThread();
	
	putLock.acquire();
	
	lock.acquire();
	
	Debug.println('0', currentThread.name + " is putting in " + obj.toString());
	
	boolean offered = queue.offer(obj);
	
	awaitingTake++;
	itemAdded.signal();
	
	while (lastRemoved != obj) {
	    itemTaken.await();
	}
	
	Debug.println('0', currentThread.name + "'s object has been taken");
	
	awaitingPut--;
	lock.release();
	
	putLock.release();
	
	return offered;
    }

    /**
     * Retrieves and removes the head of this queue,
     * waiting if necessary for another thread to insert it.
     *
     * @return the head of this queue.
     */
    public T take() { 
	NachosThread currentThread = NachosThread.currentThread();
	
	takeLock.acquire();
	
        lock.acquire();
        T obj = null;
        
	Debug.println('0', currentThread.name + " is looking for something to take");
        
        awaitingPut++;
        while (awaitingTake == 0) {
            itemAdded.await();
        }

        obj = queue.poll();

	Debug.println('0', currentThread.name + " took " + obj.toString());
        
        awaitingTake--;
        lastRemoved = obj;
        
        itemTaken.broadcast();
        
        lock.release();
        takeLock.release();
        return obj;
    }

    /**
     * Adds an element to this queue, if there is a thread currently
     * waiting to remove it, otherwise returns immediately.
     * 
     * @param e  The element to add.
     * @return  true if the element was successfully added, false if the element
     * was not added.
     */
    @Override
    public boolean offer(T e) {
	boolean success = false;
	lock.acquire();
	awaitingTake++;
	if(awaitingPut > 0){
	    itemAdded.signal();
	}else{
	    return false;
	}
	awaitingPut--;
	if(lastRemoved == e){
	    success = true;
	}
	lock.release();
	return success; 
    }
    
    /**
     * Retrieves and removes the head of this queue, if another thread
     * is currently making an element available.
     * 
     * @return  the head of this queue, or null if no element is available.
     */
    @Override
    public T poll() { 
	T obj = null;
	lock.acquire();
	if(awaitingTake > 0){
	    obj = queue.poll();
	}
	awaitingTake--;
        lastRemoved = obj;
        itemTaken.broadcast();
	lock.release();
	return obj; 
    }
    
    /**
     * Always returns null.
     *
     * @return  null
     */
    @Override
    public T peek() { 
	return null; 
    }
    
    /**
     * Always returns true.
     * 
     * @return true
     */
    @Override
    public boolean isEmpty() { 
	return true;
    }

    // The following methods are to be implemented for the second
    // part of the assignment.

    /**
     * Adds an element to this queue, waiting up to the specified
     * timeout for a thread to be ready to remove it.
     * 
     * @param e  The element to add.
     * @param timeout  The length of time (in "ticks") to wait for a
     * thread to be ready to remove the element, before giving up and
     * returning false.
     * @return  true if the element was successfully added, false if the element
     * was not added.
     */
    public boolean offer(T e, int timeout) { return false; }
    
    /**
     * Retrieves and removes the head of this queue, waiting up to the
     * specified timeout for a thread to make an element available.
     * 
     * @param timeout  The length of time (in "ticks") to wait for a
     * thread to make an element available, before giving up and returning
     * true.
     * @return  the head of this queue, or null if no element is available.
     */
    public T poll(int timeout) { return null; }

}