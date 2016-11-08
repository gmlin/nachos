package nachos.kernel.userprog;

import java.util.Comparator;
import java.util.PriorityQueue;

public class ShortestProcessNext implements ReadyList {

    private final PriorityQueue<UserThread> queue;
    
    public ShortestProcessNext() {
	queue = new PriorityQueue<UserThread>(new Comparator<UserThread>() {

	    @Override
	    public int compare(UserThread o1, UserThread o2) {
		return o1.predictedCPU - o2.predictedCPU;
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

}
