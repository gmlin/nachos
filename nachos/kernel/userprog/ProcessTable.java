package nachos.kernel.userprog;

import java.util.HashMap;
import java.util.Map;

import nachos.Debug;
import nachos.kernel.threads.Lock;
import nachos.kernel.threads.Semaphore;

public class ProcessTable {

    private int nextSpaceId;
    private Map<AddrSpace, Integer> addrSpaceMap;
    private Map<Integer, Semaphore> joinSemaphores;
    private Map<Integer, Integer> exitValues;
    private Lock lock;
    
    public ProcessTable() {
	nextSpaceId = 0;
	addrSpaceMap = new HashMap<>();
	joinSemaphores = new HashMap<>();
	exitValues = new HashMap<>();
	lock = new Lock("Process table lock");
    }
    
    /** returns the spaceId for an existing or new AddrSpace */
    public int getOrAddSpace(AddrSpace space) {
	int spaceId;
	lock.acquire();
	if (addrSpaceMap.containsKey(space))
	    spaceId = addrSpaceMap.get(space);
	else {
	    spaceId = nextSpaceId++;
	    addrSpaceMap.put(space, spaceId);
	    Debug.println('0', "Added space " + spaceId);
	}
	lock.release();
	
	return spaceId;
    }
    
    public void removeSpace(AddrSpace space, int status) {
	int spaceId;
	lock.acquire();
	spaceId = addrSpaceMap.remove(space);
	
	exitValues.put(spaceId, status);
	
	if (joinSemaphores.containsKey(spaceId))
	    joinSemaphores.get(spaceId).V();
	lock.release();
	
	Debug.println('0', "Removed space " + spaceId);
    }
    
    public void addSemaphore(int joinId, Semaphore semaphore) {
	lock.acquire();
	joinSemaphores.put(joinId, semaphore);
	
	if (exitValues.containsKey(joinId))
	    semaphore.V();
	lock.release();
    }
    
    public int getExitValue(int id) {
	int exitValue;
	lock.acquire();
	
	if (exitValues.containsKey(id))
	    exitValue = exitValues.remove(id);
	else 
	    exitValue = -1;
	
	lock.release();
	
	return exitValue;
    }

    public void removeSemaphore(int id) {
	joinSemaphores.remove(id);
    }
}
