package nachos.kernel.filesys;

import java.util.HashMap;
import java.util.Map;

import nachos.kernel.threads.Lock;

public class FileHeaderTable {

    private Map<Integer, FileHeader> table;
    private Lock lock;
    
    public FileHeaderTable() {
	table = new HashMap<>();
	lock = new Lock("FileHeaderTable lock");
    }
    
    public void put(int sector, FileHeader header) {
	lock.acquire();
	if (!table.containsKey(sector))
	    table.put(sector, header);
	lock.release();
    }
    
    public FileHeader get(int sector) {
	FileHeader hdr;
	lock.acquire();
	hdr = table.get(sector);
	lock.release();
	
	return hdr;
    }
    
    public void remove(int sector) {
	lock.acquire();
	table.remove(sector);
	lock.release();
    }
    
    public boolean contains(int sector) {
	boolean contains;
	lock.acquire();
	contains = table.containsKey(sector);
	lock.release();
	return contains;
    }
}
