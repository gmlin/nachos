package nachos.kernel.devices.test;

import java.util.Random;

import nachos.Debug;
import nachos.machine.Disk;
import nachos.machine.Machine;
import nachos.machine.NachosThread;
import nachos.kernel.Nachos;
import nachos.kernel.threads.Scheduler;
import nachos.kernel.devices.DiskDriver;
import nachos.kernel.devices.SerialDriver;

public class DiskDriverTest {
    
    private static final int NUM_THREADS = 20;
    private static final int SLEEP_TIME = 5000;
    
    public static void start() {
	Debug.println('p', "Entering DiskDriverTest");
	
	DiskDriver driver = Nachos.diskDriver;
	Random random = new Random();
	
	int numSectors = driver.getNumSectors();
	int sectorSize = driver.getSectorSize();
	
	for (int i = 0; i < NUM_THREADS; i++) {
	    Nachos.scheduler.sleepThread(SLEEP_TIME);
	    
	    NachosThread thread = new NachosThread("Thread #" + i, new Runnable() {

		@Override
		public void run() {
		    int sectorNumber = random.nextInt(numSectors);
		    byte[] buffer = new byte[sectorSize];
		    
		    if (random.nextBoolean()) {
			Debug.println('0', NachosThread.currentThread().name + " requests to read from track " + driver.getTrackNumber(sectorNumber));
			driver.readSector(sectorNumber, buffer, 0);
		    }
		    else {
			Debug.println('0', NachosThread.currentThread().name + " requests to write to track " + driver.getTrackNumber(sectorNumber));
			driver.writeSector(sectorNumber, buffer, 0);
		    }
		    
		    Debug.println('0', NachosThread.currentThread().name + " request has been fulfilled");
		    Nachos.scheduler.finishThread();
		}
		
	    });
	    
	    Nachos.scheduler.readyToRun(thread);
	}
    }
}
