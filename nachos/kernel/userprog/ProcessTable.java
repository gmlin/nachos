package nachos.kernel.userprog;

import java.util.HashMap;
import java.util.Map;

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
    
    public int addSpace(AddrSpace space) {
	int spaceId;
	
	lock.acquire();
	spaceId = nextSpaceId;
	addrSpaceMap.put(spaceId, space);
	nextSpaceId++;
	lock.release();
	
	return spaceId;
    }
    
    public void removeSpaceId(int spaceId) {
	lock.acquire();
	addrSpaceMap.remove(spaceId);
	lock.release();
    }
    
    public AddrSpace getSpace(int spaceId) {
	AddrSpace space;
	
	lock.acquire();
	space = addrSpaceMap.get(spaceId);
	lock.release();
	
	return space;
    }
}
