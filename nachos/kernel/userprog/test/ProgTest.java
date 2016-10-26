// ProgTest.java
//	Test class for demonstrating that Nachos can load
//	a user program and execute it.  
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// Copyright (c) 2003 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.

package nachos.kernel.userprog.test;

import nachos.Debug;
import nachos.Options;
import nachos.machine.CPU;
import nachos.machine.NachosThread;
import nachos.kernel.Nachos;
import nachos.kernel.userprog.AddrSpace;
import nachos.kernel.userprog.UserProgram;
import nachos.kernel.userprog.UserThread;
import nachos.kernel.filesys.OpenFile;

/**
 * This is a test class for demonstrating that Nachos can load a user
 * program and execute it.
 * 
 * @author Thomas Anderson (UC Berkeley), original C++ version
 * @author Peter Druschel (Rice University), Java translation
 * @author Eugene W. Stark (Stony Brook University)
 */
public class ProgTest {

    /**
     * Entry point for the test.  Command line arguments are checked for
     * the name of the program to execute, then the test is started by
     * creating a new ProgTest object.
     */
    public static void start() {
	Debug.ASSERT(Nachos.options.FILESYS_REAL || Nachos.options.FILESYS_STUB,
			"A filesystem is required to execute user programs");
	final int[] count = new int[1];
	Nachos.options.processOptions
		(new Options.Spec[] {
			new Options.Spec
				("-x",
				 new Class[] {String.class},
				 "Usage: -x <executable file>",
				 new Options.Action() {
				    public void processOption(String flag, Object[] params) {
					new UserProgram((String)params[0], count[0]++).start(null);
				    }
				 })
		 });
    }
}
