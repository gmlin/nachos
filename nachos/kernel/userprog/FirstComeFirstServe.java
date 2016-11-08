package nachos.kernel.userprog;

import nachos.util.FIFOQueue;
import nachos.util.Queue;

public class FirstComeFirstServe implements ReadyList {

    private final Queue<UserThread> queue;
    
    public FirstComeFirstServe() {
	queue = new FIFOQueue<UserThread>();
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

}
