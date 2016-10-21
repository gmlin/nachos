package nachos.kernel.userprog;

import java.util.ArrayDeque;
import java.util.Deque;

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
    
    /* Return a freed page or the next unused page. If no free/unused pages, return -1 */
    public int getUnusedPage() {
	int pageToReturn = -1;
	lock.acquire();
	if (freedPages.isEmpty() && nextPageToAllocate < Machine.NumPhysPages)
	    pageToReturn = nextPageToAllocate++;
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
}
