package nachos.kernel.devices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import nachos.machine.Machine;

/*
 * ConsoleDriver 0 is default console and is not in the pool.
 */
public class ConsoleManager {
    private ArrayList<ConsoleDriver> cd = new ArrayList<ConsoleDriver>(); 
    private Map<ConsoleDriver, Boolean> allocated = new HashMap<ConsoleDriver, Boolean>();				   

    /*
     * creates a pool of some number of consoles.
     */
    public ConsoleManager(int consoles) {
	for (int i = 1; i < consoles; i++) { // console 0 is skipped
	    ConsoleDriver newDriver = new ConsoleDriver(Machine.getConsole(i));
	    cd.add(newDriver);
	    allocated.put(newDriver, false);
	}
    }

    /*
     * looks through the list of consoles until it finds an unallocated console.
     */
    public ConsoleDriver getConsole() {
	for (ConsoleDriver consoled : cd) {
	    if (allocated.get(consoled).equals(false)) {
		allocated.put(consoled, true);
		return consoled;
	    }
	}
	return null; //all consoles allocated, should prob throw some error here
    }

    public void freeConsole(ConsoleDriver driver) {
	allocated.put(driver, false);	//set it back to unallocated
    }

}
