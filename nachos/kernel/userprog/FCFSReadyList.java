package nachos.kernel.userprog;

import nachos.machine.NachosThread;
import nachos.util.FIFOQueue;
import nachos.util.Queue;

public class FCFSReadyList implements ReadyList {

    private final Queue<NachosThread> queue;
    
    public FCFSReadyList() {
	queue = new FIFOQueue<NachosThread>();
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
	return queue.poll();
    }

    @Override
    public boolean isEmpty() {
	return queue.isEmpty();
    }

}
