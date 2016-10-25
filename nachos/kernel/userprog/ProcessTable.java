package nachos.kernel.userprog;

import java.util.HashMap;
import java.util.Map;

import nachos.Debug;
import nachos.kernel.threads.Lock;

public class ProcessTable {

    private int nextSpaceId;
    private Map<Integer, AddrSpace> addrSpaceMap;
    private Lock lock;
    
    public ProcessTable() {
	nextSpaceId = 0;
	addrSpaceMap = new HashMap<>();
	lock = new Lock("Process table lock");
    }
    
    /** returns the spaceId for a new process */
    public int addProcess(AddrSpace space) {
	int spaceId;
	lock.acquire();
	spaceId = nextSpaceId++;
	addrSpaceMap.put(spaceId, space);
	Debug.println('0', "Added process " + spaceId);
	lock.release();
	
	return spaceId;
    }
    
    public void removeProcess(int spaceId) {
	lock.acquire();
	addrSpaceMap.remove(spaceId);
	lock.release();
	
	Debug.println('0', "Removed process " + spaceId);
    }
    
}
