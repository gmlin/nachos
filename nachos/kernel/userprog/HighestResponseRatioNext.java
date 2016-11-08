package nachos.kernel.userprog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import nachos.machine.InterruptHandler;
import nachos.machine.Machine;
import nachos.machine.Timer;

public class HighestResponseRatioNext implements ReadyList {

    private final List<UserThread> userThreadList;
    private int currentTime;
    
    public HighestResponseRatioNext() {
	userThreadList = new LinkedList<>();
	currentTime = 0;
	Timer timer = Machine.getTimer(0);
	timer.setHandler(new HighestResponseRatioNextInterruptHandler(timer));
    }
    
    private double responseRatio(UserThread thread) {
	return (currentTime + thread.timeAdded) / thread.timeAdded;
    }
    
    @Override
    public boolean offer(UserThread e) {
	e.timeAdded = currentTime;
	userThreadList.add(e);
	
	Collections.sort(userThreadList, new Comparator<UserThread>() {

	    @Override
	    public int compare(UserThread o1, UserThread o2) {
		if (responseRatio(o1) < responseRatio(o2))
		    return -1;
		return 1;
	    }
	    
	});
	
	return true;
    }

    @Override
    public UserThread peek() {
	if (userThreadList.isEmpty())
	    return null;
	return userThreadList.get(0);
    }

    @Override
    public UserThread poll() {
	if (userThreadList.isEmpty())
	    return null;
	return userThreadList.remove(0);
    }

    @Override
    public boolean isEmpty() {
	return userThreadList.isEmpty();
    }
    
    class HighestResponseRatioNextInterruptHandler implements InterruptHandler {

	private final Timer timer;
	
	public HighestResponseRatioNextInterruptHandler(Timer timer) {
	    this.timer = timer;
	}
	
	@Override
	public void handleInterrupt() {
	    currentTime += timer.interval;
	}
    }
}
