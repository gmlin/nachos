package nachos.kernel.userprog;

import java.util.ArrayDeque;
import java.util.Deque;

import nachos.Debug;
import nachos.kernel.threads.Lock;
import nachos.machine.Machine;

public class MemoryManager {
    
    private int nextPageToAllocate;
    private Deque<Integer> freedPages;
    private Lock lock;
    
    public MemoryManager() {
	nextPageToAllocate = 0;
	freedPages = new ArrayDeque<>();
	lock = new Lock("Memory manager lock");
    }
    
    /* Return a freed page or the next unallocated page. Returns -1 if ran out of memory. */
    public int getUnusedPage() {
	int pageToReturn = -1;
	
	lock.acquire();
	if (freedPages.isEmpty()) {
	    Debug.ASSERT((nextPageToAllocate <= Machine.NumPhysPages), "Memory manager: ran out of memory");
	    if (nextPageToAllocate < Machine.NumPhysPages)
		pageToReturn = nextPageToAllocate++;
	}
	else
	    pageToReturn = freedPages.pop();
	
	lock.release();
	
	return pageToReturn;
    }
    
    public void freePage(int page) {
	lock.acquire();
	freedPages.push(page);
	lock.release();
    }
    
    public int getPageMemoryIndex(int page) {
	return page * Machine.PageSize;
    }
}
