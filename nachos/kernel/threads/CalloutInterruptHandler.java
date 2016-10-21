package nachos.kernel.threads;

import nachos.kernel.Nachos;
import nachos.machine.InterruptHandler;

public class CalloutInterruptHandler implements InterruptHandler {
    
    @Override
    public void handleInterrupt() {
	Callout callout = Nachos.callout;
	callout.performCallouts();
    }

}
