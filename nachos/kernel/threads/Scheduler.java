// Scheduler.java
//
//      readyToRun -- place a thread on the ready list and make it runnable.
//	finish -- called when a thread finishes, to clean up
//	yield -- relinquish control over the CPU to another ready thread
//	sleep -- relinquish control over the CPU, but thread is now blocked.
//		In other words, it will not run again, until explicitly 
//		put back on the ready queue.
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// Copyright (c) 2003 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.

package nachos.kernel.threads;

import nachos.Debug;
import nachos.kernel.Nachos;
import nachos.kernel.userprog.FeedbackScheduling;
import nachos.kernel.userprog.FirstComeFirstServe;
import nachos.kernel.userprog.HighestResponseRatioNext;
import nachos.kernel.userprog.ReadyList;
import nachos.kernel.userprog.RoundRobin;
import nachos.kernel.userprog.ShortestProcessNext;
import nachos.kernel.userprog.ShortestRemainingTime;
import nachos.kernel.userprog.UserThread;
import nachos.machine.CPU;
import nachos.machine.Machine;
import nachos.machine.NachosThread;
import nachos.machine.Timer;
import nachos.machine.InterruptHandler;
import nachos.util.FIFOQueue;
import nachos.util.Queue;

/**
 * The scheduler is responsible for maintaining a list of threads that
 * are ready to run and for choosing the next thread to run.  It also
 * maintains a list of CPUs that can be used to run threads.
 * Most of the public methods of this class implicitly operate on the
 * currently executing thread, with the exception of readyToRun,
 * which takes the thread to be placed in the ready list as an explicit
 * parameter.
 *
 * Mutual exclusion on the scheduler state uses two mechanisms:
 * (1) Disabling interrupts on the current CPU to prevent a timer
 * interrupt from re-entering the scheduler code.
 * (2) The current CPU obtains a spin lock in order to prevent concurrent
 * access to the scheduler state by other CPUs.
 * If there is just one CPU, then (1) would be enough.
 * 
 * The scheduling policy implemented here is very simple:
 * threads that are ready to run are maintained in a FIFO queue and are
 * dispatched in order onto the first available CPU.
 * Scheduling may be preemptive or non-preemptive, depending on whether
 * timers are initialized for time-slicing.
 * 
 * @author Thomas Anderson (UC Berkeley), original C++ version
 * @author Peter Druschel (Rice University), Java translation
 * @author Eugene W. Stark (Stony Brook University)
 */
public class Scheduler {

    /** Queue of kernel threads that are ready to run, but not running. */
    private final Queue<NachosThread> kernelReadyList;

    private final ReadyList userReadyList;
    
    /** Queue of CPUs that are idle. */
    private final Queue<CPU> cpuList;
    
    /** Terminated thread awaiting reclamation of its stack. */
    private volatile NachosThread threadToBeDestroyed;

    /** Spin lock for mutually exclusive access to scheduler state. */
    private final SpinLock mutex = new SpinLock("scheduler mutex");

    /**
     * Initialize the scheduler.
     * Set the list of ready but not running threads to empty.
     * Initialize the list of CPUs to contain all the available CPUs.
     * 
     * @param firstThread  The first NachosThread to run.
     */
    public Scheduler(NachosThread firstThread) {
	kernelReadyList = new FIFOQueue<NachosThread>();
	cpuList = new FIFOQueue<CPU>();
	
	userReadyList = new FirstComeFirstServe();

	Debug.println('t', "Initializing scheduler");

	// Add all the CPUs to the idle CPU list, and start their time-slice timers,
	// if we are using them.
	for(int i = 0; i < Machine.NUM_CPUS; i++) {
	    CPU cpu = Machine.getCPU(i);
	    cpuList.offer(cpu);
	    if(Nachos.options.CPU_TIMERS) {
		Timer timer = cpu.timer;
		timer.setHandler(new TimerInterruptHandler(timer));
		if(Nachos.options.RANDOM_YIELD)
		    timer.setRandom(true);
		timer.start();
	    }
	}

	// Dispatch firstThread on the first CPU.
	CPU firstCPU = cpuList.poll();
	firstCPU.dispatch(firstThread);
    };

    /**
     * Stop the timers on all CPUs, in preparation for shutdown.
     */
    public void stop() {
	for(int i = 0; i < Machine.NUM_CPUS; i++) {
	    CPU cpu = Machine.getCPU(i);
	    cpu.timer.stop();
	}
    }

    /**
     * Mark a thread as ready, but not running, and put it on the ready list
     * for later scheduling onto a CPU.
     * If there are idle CPUs then threads are dispatched onto CPUs until either
     * all CPUs are in use or there are no more threads are ready to run.
     * 
     * It is assumed that multiple concurrent calls of this method will not be
     * made with the same thread as parameter, as that could result in the thread
     * being added more than once to the ready queue, in addition to introducing
     * the possibility of races in the setting of the thread status.
     *
     * @param thread The thread to be put on the ready list.
     */
    public void readyToRun(NachosThread thread) {
	int oldLevel = CPU.setLevel(CPU.IntOff);
	mutex.acquire();
	makeReady(thread);
	dispatchIdleCPUs();
	mutex.release();
	CPU.setLevel(oldLevel);
    }

    /**
     * Mark a thread as ready, but not running, and put it on the ready list
     * for later scheduling onto a CPU.
     * No attempt is made to dispatch threads on idle CPUs.
     * 
     * This internal version of readyToRun assumes that interrupts are disabled
     * and that the scheduler mutex is held.
     * It is assumed that multiple concurrent calls of this method will not be
     * made with the same thread as parameter.  Under that assumption, it is not
     * necessary to lock the thread object itself before changing its status to
     * READY, because any other changes to the thread status are either made by
     * the thread itself (which is currently not running), or in the process of
     * dispatching the thread, which is done with the scheduler mutex held.
     *
     * @param thread The thread to be put on the ready list.
     */
    private void makeReady(NachosThread thread) {
	Debug.ASSERT(CPU.getLevel() == CPU.IntOff && mutex.isLocked());
	
	Debug.println('t', "Putting thread on ready list: " + thread.name);

	thread.setStatus(NachosThread.READY);
	
	if (thread instanceof UserThread)
	    userReadyList.offer((UserThread)thread);
	else
	    kernelReadyList.offer(thread);
    }

    /**
     * If there are idle CPUs and threads ready to run, dispatch threads on CPUs
     * until either all CPUs are in use or no more threads are ready to run.
     * Assumes that interrupts have been disabled and that the scheduler mutex
     * is held.
     */
    private void dispatchIdleCPUs() {
	Debug.ASSERT(CPU.getLevel() == CPU.IntOff && mutex.isLocked());
	while((!kernelReadyList.isEmpty() || !userReadyList.isEmpty()) && !cpuList.isEmpty()) {
	    NachosThread thread;
	    if (!kernelReadyList.isEmpty())
		thread = kernelReadyList.poll();
	    else
		thread = userReadyList.poll();
	    CPU cpu = cpuList.poll();
	    Debug.println('t', "Dispatching " + thread.name + " on " + cpu.name);
	    cpu.dispatch(thread);
	    // The current CPU is not relinquished here -- immediate return.
	}
    }

    /**
     * Return the next thread to be scheduled onto a CPU.
     * If there are no ready threads, return null.
     * Side effect: thread is removed from the ready list.
     * Assumes that interrupts have been disabled.
     *
     * @return the thread to be scheduled onto a CPU.
     */
    private NachosThread findNextToRun() {
	Debug.ASSERT(CPU.getLevel() == CPU.IntOff);
	mutex.acquire();
	NachosThread result = kernelReadyList.poll();
	if (result == null)
	    result = userReadyList.poll();
	mutex.release();
	return result;
    }

    /**
     * Yield the current CPU, either to another thread, or else leave it idle.
     * Save the state of the current thread, and if a new thread is to be run,
     * dispatch it using the machine-dependent dispatcher routine: NachosThread.setCPU.
     * The thread that is yielding will either go back into the ready list for
     * rescheduling, or it will block, leaving the scheduler for an indefinite period
     * until something has again made it ready to run.
     * 
     * This method must be called with interrupts disabled.
     * When it eventually returns, the same will again be true.
     *
     * @param status  The status desired by the currently executing thread.
     * If RUNNING, then the thread will be put in the ready list and made READY,
     * unless there is no other thread to run, in which case it will be left RUNNING.
     * If BLOCKED the thread will be set to that status and will relinquish the CPU
     * to the next thread to run.
     * If FINISHED, it is assumed that the thread has already been set to that status,
     * and the thread will relinquish the CPU to the next thread to run.
     * @param  toRelease  If non-null, a spinlock held by the caller that is to be released
     * atomically with relinquishing the CPU.
     */
    private void yieldCPU(int status, SpinLock toRelease) {
	Debug.ASSERT(CPU.getLevel() == CPU.IntOff);
	CPU currentCPU = CPU.currentCPU();
	NachosThread currentThread = NachosThread.currentThread();
	NachosThread nextThread = findNextToRun();

	// If the current thread wants to keep running and there is no other thread to run,
	// do nothing.
	if(status == NachosThread.RUNNING && nextThread == null) {
	    Debug.println('t', "No other thread to run -- " + currentThread.name
		    			+ " continuing");
	    return;
	}
	Debug.println('t', "Next thread to run: "
		+ (nextThread == null ? "(none)" : nextThread.name));

	// The current thread will be suspending -- save its context.
	currentThread.saveState();

	mutex.acquire();
	if(toRelease != null)
	    toRelease.release();
	if(nextThread != null) {
	    // Switch the CPU from currentThread to nextThread.

	    Debug.println('t', "Switching " + CPU.getName() +
		    " from " + currentThread.name +
		    " to " + nextThread.name);

	    if(status == NachosThread.RUNNING) {
		// The current thread wants to keep running -- put it back in the ready list.
		makeReady(currentThread);
	    } else {
		// Set the new status of the thread before relinquishing the CPU.
		if(status != NachosThread.FINISHED)
		    currentThread.setStatus(status);
	    }
	    CPU.switchTo(nextThread, mutex);
	} else {
	    // There is nothing for this CPU to do -- send it to the idle list.

	    Debug.println('t', "Switching " + CPU.getName() +
		    " from " + currentThread.name +
		    " to idle");

	    cpuList.offer(currentCPU);
	    if(status != NachosThread.FINISHED)
		currentThread.setStatus(status);
	    CPU.idle(mutex);
	}
	// Control returns here when currentThread has been rescheduled,
	// perhaps on a different CPU.
	Debug.ASSERT(CPU.getLevel() == CPU.IntOff);
	currentThread.restoreState();

	Debug.println('t', "Now in thread: " + currentThread.name);
    }

    /**
     * Relinquish the CPU if any other thread is ready to run.
     * If so, put the thread on the end of the ready list, so that
     * it will eventually be re-scheduled.
     *
     * NOTE: returns immediately if no other thread on the ready queue.
     * Otherwise returns when the thread eventually works its way
     * to the front of the ready list and gets re-scheduled.
     *
     * NOTE: we disable interrupts and acquire the scheduler mutex,
     * so that looking at the thread on the front of the ready list,
     * and switching to it, can be done atomically.  On return, we release
     * the scheduler mutex and re-set the interrupt level to its
     * original state.  This means this method will work properly no
     * matter whether interrupts are enabled or disabled when it is called,
     * but it should never be called with the scheduler mutex already locked.
     *
     * Similar to sleep(), but a little different.
     */
    public void yieldThread () {
	int oldLevel = CPU.setLevel(CPU.IntOff);

	Debug.println('t', "Yielding thread: " + NachosThread.currentThread().name);

	yieldCPU(NachosThread.RUNNING, null);
	// Control returns here when currentThread is rescheduled.

	CPU.setLevel(oldLevel);
    }

    /**
     * Relinquish the CPU, because the current thread is going to block
     * (i.e. either wait on a synchronization variable or, if the thread is finishing,
     * await final destruction).  If the thread is not finishing, then arrangements
     * should have been made for this thread to eventually be placed back on the ready
     * list, so that it can be re-scheduled.  If the thread is finishing, then
     * it will remain blocked until it is destroyed.
     *
     * NOTE: this method assumes interrupts are disabled, so that there can't be a time
     * slice between pulling the first thread off the ready list, and switching to it.
     * 
     * @param  toRelease  A spinlock held by the caller that is to be released atomically
     * with relinquishing the CPU.
     */
    public void sleepThread (SpinLock toRelease) {
	NachosThread currentThread = NachosThread.currentThread();
	Debug.ASSERT(CPU.getLevel() == CPU.IntOff);

	Debug.println('t', "Sleeping thread: " + currentThread.name);

	yieldCPU(NachosThread.BLOCKED, toRelease);
	// Control returns here when currentThread is rescheduled.
	// The caller is responsible for re-enabling interrupts.
    }

    public void sleepThread(int ticks) {
	NachosThread currentThread = NachosThread.currentThread();
	Semaphore semaphore = new Semaphore("semaphore", 0);
	Callout callout = Nachos.callout;
	
	Debug.println('0', "*** thread " + currentThread.name + " sleeping for " + ticks + " ticks" );
	
	callout.schedule(new Runnable() {

	    @Override
	    public void run() {
		semaphore.V();
	    }
	    
	}, ticks);
	
	semaphore.P();
	
	Debug.println('0', "*** thread " + currentThread.name + " woke up");
    }
    
    /**
     * Called by a thread to terminate itself.
     * A thread can't completely destroy itself, because it needs some
     * resources (e.g. a stack) as long as it is running.  So it is the
     * responsibility of the next thread to run to finish the job.
     */
    public void finishThread() {
	CPU.setLevel(CPU.IntOff);
	NachosThread currentThread = NachosThread.currentThread();

	Debug.println('t', "Finishing thread: " + currentThread.name);

	// We have to make sure the thread has been set to the FINISHED state
	// before making it the thread to be destroyed, because we don't want
	// someone to try to destroy a thread that is not FINISHED.
	currentThread.setStatus(NachosThread.FINISHED);
	
	// Delete the carcass of any thread that died previously.
	// This ensures that there is at most one dead thread ever waiting
	// to be cleaned up.
	mutex.acquire();
	if (threadToBeDestroyed != null) {
	    threadToBeDestroyed.destroy();
	    threadToBeDestroyed = null;
	}
	threadToBeDestroyed = currentThread;
	mutex.release();

	yieldCPU(NachosThread.FINISHED, null);
	// not reached

	// Interrupts will be re-enabled when the next thread runs or the
	// current CPU goes idle.
    }

    /**
     * Interrupt handler for the time-slice timer.  A timer is set up to
     * interrupt the CPU periodically (once every Timer.DefaultInterval ticks).
     * The handleInterrupt() method is called with interrupts disabled each
     * time there is a timer interrupt.
     */
    private static class TimerInterruptHandler implements InterruptHandler {

	/** The Timer device this is a handler for. */
	private final Timer timer;
	
	/**
	 * Initialize an interrupt handler for a specified Timer device.
	 * 
	 * @param timer  The device this handler is going to handle.
	 */
	public TimerInterruptHandler(Timer timer) {
	    this.timer = timer;
	}

	public void handleInterrupt() {
	    Debug.println('i', "Timer interrupt: " + timer.name);
	    // Note that instead of calling yield() directly (which would
	    // suspend the interrupt handler, not the interrupted thread
	    // which is what we wanted to context switch), we set a flag
	    // so that once the interrupt handler is done, it will appear as 
	    // if the interrupted thread called yield at the point it is 
	    // was interrupted.
	    yieldOnReturn();
	}

	/**
	 * Called to cause a context switch (for example, on a time slice)
	 * in the interrupted thread when the handler returns.
	 *
	 * We can't do the context switch right here, because that would switch
	 * out the interrupt handler, and we want to switch out the 
	 * interrupted thread.  Instead, we set a hook to kernel code to be executed
	 * when the current handler returns.
	 */
	private void yieldOnReturn() {
	    Debug.println('i', "Yield on interrupt return requested");
	    CPU.setOnInterruptReturn
	    (new Runnable() {
		public void run() {
		    if(NachosThread.currentThread() != null) {
			Debug.println('t', "Yielding current thread on interrupt return");
			Nachos.scheduler.yieldThread();
		    } else {
			Debug.println('i', "No current thread on interrupt return, skipping yield");
		    }
		}
	    });
	}

    }
}
