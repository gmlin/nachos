package nachos.kernel.userprog;

import java.util.HashMap;
import java.util.Map;

import nachos.Debug;
import nachos.kernel.threads.Lock;

public class ProcessTable {

    private int nextSpaceId;
    private Map<AddrSpace, Integer> addrSpaceMap;
    private Lock lock;
    
    public ProcessTable() {
	nextSpaceId = 0;
	addrSpaceMap = new HashMap<>();
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
    
    public void removeSpace(AddrSpace space) {
	int spaceId;
	lock.acquire();
	spaceId = addrSpaceMap.remove(space);
	lock.release();
	
	Debug.println('0', "Removed space " + spaceId);
    }
    
}
