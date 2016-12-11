// AddrSpace.java
//	Class to manage address spaces (executing user programs).
//
//	In order to run a user program, you must:
//
//	1. link with the -N -T 0 option 
//	2. run coff2noff to convert the object file to Nachos format
//		(Nachos object code format is essentially just a simpler
//		version of the UNIX executable object code format)
//	3. load the NOFF file into the Nachos file system
//		(if you haven't implemented the file system yet, you
//		don't need to do this last step)
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// Copyright (c) 2003 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.

package nachos.kernel.userprog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nachos.Debug;
import nachos.machine.CPU;
import nachos.machine.MIPS;
import nachos.machine.Machine;
import nachos.machine.NachosThread;
import nachos.machine.TranslationEntry;
import nachos.noff.NoffHeader;
import nachos.kernel.Nachos;
import nachos.kernel.filesys.OpenFile;
import nachos.kernel.threads.Lock;

/**
 * This class manages "address spaces", which are the contexts in which
 * user programs execute.  For now, an address space contains a
 * "segment descriptor", which describes the the virtual-to-physical
 * address mapping that is to be used when the user program is executing.
 * As you implement more of Nachos, it will probably be necessary to add
 * other fields to this class to keep track of things like open files,
 * network connections, etc., in use by a user program.
 *
 * NOTE: Most of what is in currently this class assumes that just one user
 * program at a time will be executing.  You will have to rewrite this
 * code so that it is suitable for multiprogramming.
 * 
 * @author Thomas Anderson (UC Berkeley), original C++ version
 * @author Peter Druschel (Rice University), Java translation
 * @author Eugene W. Stark (Stony Brook University)
 */
public class AddrSpace {

  /** Page table that describes a virtual-to-physical address mapping. */
  // private TranslationEntry pageTable[];

  /** Default size of the user stack area -- increase this as necessary! */
  private static final int UserStackSize = 1024;
  
  private static final int NumStackPages = (int)Math.ceil((double)UserStackSize / Machine.PageSize);
  
  /** The page tables of the UserThreads that use this address space */
  private Map<UserThread, TranslationEntry[]> threadPageTables;

  /** The part of the page table common to all UserThreads in this address space */
  private TranslationEntry[] basePageTable;
  
  private Lock lock;
  
  /**
   * Create a new address space.
   */
  public AddrSpace() {
      threadPageTables = new HashMap<>();
      lock = new Lock("Page tables lock");
  }

  /**
   * Load the program from a file "executable", and set everything
   * up so that we can start executing user instructions.
   *
   * Assumes that the object code file is in NOFF format.
   *
   * First, set up the translation from program memory to physical 
   * memory.  For now, this is really simple (1:1), since we are
   * only uniprogramming.
   *
   * @param executable The file containing the object code to 
   * 	load into memory
   * @return -1 if an error occurs while reading the object file,
   *    otherwise 0.
   */
  public int exec(OpenFile executable) {
    NoffHeader noffH;
    long size;
    
    if((noffH = NoffHeader.readHeader(executable)) == null)
	return(-1);

    // how big is address space?
    size = roundToPage(noffH.code.size)
	     + roundToPage(noffH.initData.size + noffH.uninitData.size)
	     + UserStackSize;	// we need to increase the size
    				// to leave room for the stack
    int numPages = (int)(size / Machine.PageSize);
    Debug.ASSERT((numPages <= Machine.NumPhysPages),// check we're not trying
		 "AddrSpace constructor: Not enough memory!");
                                                // to run anything too big --
						// at least until we have
						// virtual memory

    // first, set up the translation 
    TranslationEntry[] pageTable = new TranslationEntry[numPages];
    
    threadPageTables.put((UserThread)NachosThread.currentThread(), pageTable);
    
    for (int i = 0; i < numPages; i++) {
      initializePage(pageTable, i);
    }
    
    // then, copy in the code and data segments into memory
    if (noffH.code.size > 0) {
      Debug.println('a', "Initializing code segment, at " +
	    noffH.code.virtualAddr + ", size " +
	    noffH.code.size);

      executable.seek(noffH.code.inFileAddr);
      
      for (int i = 0; i < noffH.code.size; i++)
	  executable.read(Machine.mainMemory, translate(noffH.code.virtualAddr + i), 1);
    }

    if (noffH.initData.size > 0) {
      Debug.println('a', "Initializing data segment, at " +
	    noffH.initData.virtualAddr + ", size " +
	    noffH.initData.size);

      executable.seek(noffH.initData.inFileAddr);
      
      for (int i = 0; i < noffH.initData.size; i++)
	  executable.read(Machine.mainMemory, translate(noffH.initData.virtualAddr + i), 1);
    }
    
    basePageTable = Arrays.copyOf(pageTable, numPages - NumStackPages);
    
    return(0);
  }

private void wipePage(TranslationEntry[] pageTable, int virtualPage) {
    int physicalPageAddr = getPageAddr(pageTable[virtualPage].physicalPage);
    for (int j = 0; j < Machine.PageSize; j++) {
        Machine.mainMemory[physicalPageAddr + j] = (byte)0;
    } // Zero out the entire address space, to zero the uninitialized data 
    // segment and the stack segment.
}

private void initializePage(TranslationEntry[] pageTable, int virtualPage) {
      MemoryManager memoryManager = Nachos.memoryManager;
    
      pageTable[virtualPage] = new TranslationEntry();
      pageTable[virtualPage].virtualPage = virtualPage;
      pageTable[virtualPage].physicalPage = memoryManager.getUnusedPage();
      pageTable[virtualPage].valid = true;
      pageTable[virtualPage].use = false;
      pageTable[virtualPage].dirty = false;
      pageTable[virtualPage].readOnly = false;  // if code and data segments live on
				      // separate pages, we could set code 
				      // pages to be read-only
      wipePage(pageTable, virtualPage);
      
      Debug.println('0', NachosThread.currentThread().name + " " +  virtualPage + " virtual to " + pageTable[virtualPage].physicalPage + " physical");
}

  /**
   * Initialize the user-level register set to values appropriate for
   * starting execution of a user program loaded in this address space.
   *
   * We write these directly into the "machine" registers, so
   * that we can immediately jump to user code.
   */
  public void initRegisters() {
    int i;
   
    for (i = 0; i < MIPS.NumTotalRegs; i++)
      CPU.writeRegister(i, 0);

    // Initial program counter -- must be location of "Start"
    CPU.writeRegister(MIPS.PCReg, 0);	

    // Need to also tell MIPS where next instruction is, because
    // of branch delay possibility
    CPU.writeRegister(MIPS.NextPCReg, 4);

    // Set the stack register to the end of the segment.
    // NOTE: Nachos traditionally subtracted 16 bytes here,
    // but that turns out to be to accomodate compiler convention that
    // assumes space in the current frame to save four argument registers.
    // That code rightly belongs in start.s and has been moved there.
    int sp = getCurrentPageTable().length * Machine.PageSize;
    CPU.writeRegister(MIPS.StackReg, sp);
    Debug.println('a', "Initializing stack register to " + sp);
  }

  /**
   * On a context switch, save any machine state, specific
   * to this address space, that needs saving.
   *
   * For now, nothing!
   */
  public void saveState() {
    Debug.println('0', NachosThread.currentThread().name + " saving state");
  }

  /**
   * On a context switch, restore any machine state specific
   * to this address space.
   *
   * For now, just tell the machine where to find the page table.
   */
  public void restoreState() {
    Debug.println('0', NachosThread.currentThread().name + " restoring state");
    CPU.setPageTable(getCurrentPageTable());
  }
  
  public String readString(int virtualAddr) {
      int virtualPage = getPage(virtualAddr);
      int offset = getOffset(virtualAddr);
      StringBuilder sb = new StringBuilder();
      boolean done = false;
      
      while (!done) {
	  for (int i = offset; i < Machine.PageSize; i++) {
	      byte curr = getByteAt(virtualPage, i);
	      if (curr == 0) {
		  done = true;
		  break;
	      }
	      else
		  sb.append((char)curr);
	  }
	  offset = 0;
	  virtualPage++;
      }
      
      return sb.toString();
  }
  
  /**
   * Utility method for rounding up to a multiple of CPU.PageSize;
   */
  private long roundToPage(long size) {
    return(Machine.PageSize * ((size+(Machine.PageSize-1))/Machine.PageSize));
  }
  
  public int getPageAddr(int page) {
      return page * Machine.PageSize;
  }
  
  public int getPage(int addr) {
      return addr / Machine.PageSize;
  }
  
  public int getOffset(int addr) {
      return addr % Machine.PageSize;
  }
  
  public byte getByteAt(int virtualPage, int offset) {
      return Machine.mainMemory[translate(getPageAddr(virtualPage) + offset)];
  }
  
  public void exit(int status) {
      for (int i = 0; i < basePageTable.length; i++) {
	  free(basePageTable, i);
      }
      
      Nachos.processTable.removeSpace(this, status);
  }
  
  public int translate(int virtualAddr) {
      int virtualPage = virtualAddr / Machine.PageSize;
      int offset = virtualAddr % Machine.PageSize;
      int physicalPage = getCurrentPageTable()[virtualPage].physicalPage;
      int physicalAddr = getPageAddr(physicalPage) + offset;
      
      return physicalAddr;
  }

  public void addUserThread(UserThread thread) {
    TranslationEntry[] pageTable = Arrays.copyOf(basePageTable, basePageTable.length + NumStackPages);
    Debug.println('0', thread.name + " added to AddrSpace " + thread.spaceId);
    for (int i = basePageTable.length; i < pageTable.length; i++) {
	initializePage(pageTable, i);
    }
    
    lock.acquire();
    threadPageTables.put(thread, pageTable);
    lock.release();
  }
  
  public void removeUserThread(UserThread thread) {
    TranslationEntry[] pageTable;
    Debug.println('0', thread.name + " removed from AddrSpace " + thread.spaceId);
    lock.acquire();
    pageTable = threadPageTables.remove(thread);
    lock.release();
    
    for (int i = basePageTable.length; i < pageTable.length; i++) {
	free(pageTable, i);
    }
  }

private void free(TranslationEntry[] pageTable, int virtualPage) {
    Nachos.memoryManager.freePage(pageTable[virtualPage].physicalPage);
    Debug.println('0', "Physical page " + pageTable[virtualPage].physicalPage + " freed");
}

  private TranslationEntry[] getCurrentPageTable() {
      TranslationEntry[] pageTable;
      
      lock.acquire();
      pageTable = threadPageTables.get(NachosThread.currentThread());
      lock.release();
      
      return pageTable;
  }
  
  public boolean hasNoUserThreads() {
      boolean hasNoUserThreads;
      
      lock.acquire();
      hasNoUserThreads = threadPageTables.isEmpty();
      lock.release();
      
      return hasNoUserThreads;
  }
  
  public void extendPageTable(int addr) {
      int virtualPage = addr / Machine.PageSize;
      TranslationEntry[] extendedPageTable = new TranslationEntry[virtualPage + 1];
      TranslationEntry[] pageTable = getCurrentPageTable();
      for (int i = 0; i < pageTable.length; i++) {
	  extendedPageTable[i] = pageTable[i];
      }
      
      for (int i = pageTable.length; i <= virtualPage; i++) {
	  extendedPageTable[i] = new TranslationEntry();
	  extendedPageTable[i].virtualPage = i;
	  extendedPageTable[i].physicalPage = -1;
	  extendedPageTable[i].valid = false;
	  extendedPageTable[i].use = false;
	  extendedPageTable[i].dirty = false;
	  extendedPageTable[i].readOnly = false;
      }
      
      threadPageTables.put((UserThread)NachosThread.currentThread(), extendedPageTable);
      CPU.setPageTable(extendedPageTable);
      
      Debug.println('0', "Page table extended from " + pageTable.length + " entries to " + extendedPageTable.length);
  }
  
  public void initializePage(int addr) {
      int virtualPage = addr / Machine.PageSize;
      TranslationEntry[] pageTable = getCurrentPageTable();
      initializePage(pageTable, virtualPage);
      CPU.setPageTable(pageTable);
  }
}
