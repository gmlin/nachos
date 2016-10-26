package nachos.kernel.userprog;

import nachos.Debug;
import nachos.kernel.Nachos;
import nachos.kernel.filesys.OpenFile;
import nachos.kernel.threads.Semaphore;
import nachos.machine.CPU;
import nachos.machine.MIPS;
import nachos.machine.Machine;
import nachos.machine.NachosThread;

public class UserProgram implements Runnable {

    /** The name of the program to execute. */
    private String execName;
    private UserThread userThread;
    private boolean forked;
    private int func;
    private Semaphore notifyCreator;
    
    /**
     * Start the test by creating a new address space and user thread,
     * then arranging for the new thread to begin executing the run() method
     * of this class.
     *
     * @param filename The name of the program to execute.
     */
    
    /** Fork program */
    public UserProgram(UserThread thread, int func) {
	execName = "Fork(" + thread.name + ")";
	AddrSpace space = thread.space;
	userThread = new UserThread(execName, this, space);
	forked = true;
	this.func = func;
	space.addUserThread(userThread);
    }
    
    public UserProgram(String filename) {
	Debug.println('+', "starting ProgTest: " + filename);

	execName = filename;
	AddrSpace space = new AddrSpace();
	forked = false;
	userThread = new UserThread(filename, this, space);
	
    }
    
    public UserProgram(String filename, int num) {
	String name = "ProgTest"+ num + "(" + filename + ")";
	
	Debug.println('+', "starting ProgTest: " + name);

	execName = filename;
	AddrSpace space = new AddrSpace();
	forked = false;
	userThread = new UserThread(name, this, space);
    }

    /**
     * Entry point for the thread created to run the user program.
     * The specified executable file is used to initialize the address
     * space for the current thread.  Once this has been done,
     * CPU.run() is called to transfer control to user mode.
     */
    public void run() {
	if (notifyCreator != null)
	    notifyCreator.V();
	
	if (!forked) {
        	OpenFile executable;
        	if((executable = Nachos.fileSystem.open(execName)) == null) {
        	    Debug.println('+', "Unable to open executable file: " + execName);
        	    Nachos.scheduler.finishThread();
        	    return;
        	}
        
        	AddrSpace space = ((UserThread)NachosThread.currentThread()).space;
        	if(space.exec(executable) == -1) {
        	    Debug.println('+', "Unable to read executable file: " + execName);
        	    Nachos.scheduler.finishThread();
        	    return;
        	}
        	
        	space.initRegisters();		// set the initial register values
	}
	else {
	    CPU.writeRegister(MIPS.PrevPCReg,
		    CPU.readRegister(MIPS.PCReg));
	    CPU.writeRegister(MIPS.PCReg,
		    func);
	    CPU.writeRegister(MIPS.NextPCReg,
		    CPU.readRegister(MIPS.PCReg)+4);
	}
	
	userThread.space.restoreState();		// load page table register
	CPU.runUserCode();			// jump to the user progam
	Debug.ASSERT(false);		// machine->Run never returns;
	// the address space exits
	// by doing the syscall "exit"
    }
    
    public int start(Semaphore sem) {
	notifyCreator = sem;
	
	Nachos.scheduler.readyToRun(userThread);
	
	return userThread.spaceId;
    }
    
    public String getName() {
	return execName;
    }
}
