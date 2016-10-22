package nachos.kernel.userprog;

import nachos.Debug;
import nachos.kernel.Nachos;
import nachos.kernel.filesys.OpenFile;
import nachos.machine.CPU;
import nachos.machine.NachosThread;

public class UserProgram implements Runnable {

    /** The name of the program to execute. */
    private String execName;
    private UserThread userThread;
    
    /**
     * Start the test by creating a new address space and user thread,
     * then arranging for the new thread to begin executing the run() method
     * of this class.
     *
     * @param filename The name of the program to execute.
     */
    
    public UserProgram(String filename) {
	Debug.println('+', "starting ProgTest: " + filename);

	execName = filename;
	AddrSpace space = new AddrSpace();
	userThread = new UserThread(filename, this, space);
    }
    
    public UserProgram(String filename, int num) {
	String name = "ProgTest"+ num + "(" + filename + ")";
	
	Debug.println('+', "starting ProgTest: " + name);

	execName = filename;
	AddrSpace space = new AddrSpace();
	userThread = new UserThread(name, this, space);
    }

    /**
     * Entry point for the thread created to run the user program.
     * The specified executable file is used to initialize the address
     * space for the current thread.  Once this has been done,
     * CPU.run() is called to transfer control to user mode.
     */
    public void run() {
	OpenFile executable;
	System.out.println("running" + " " + execName);
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
	space.restoreState();		// load page table register

	CPU.runUserCode();			// jump to the user progam
	Debug.ASSERT(false);		// machine->Run never returns;
	// the address space exits
	// by doing the syscall "exit"
    }
    
    public int start() {
	Nachos.scheduler.readyToRun(userThread);
	userThread.space.addUserThread(userThread);
	
	return userThread.spaceId;
    }
}
