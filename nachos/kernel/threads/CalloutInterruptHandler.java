package nachos.kernel.threads;

import nachos.machine.InterruptHandler;

public class CalloutInterruptHandler implements InterruptHandler {
    
    @Override
    public void handleInterrupt() {
	Callout callout = Callout.getInstance();
	callout.updateCurrentTime();
	callout.performCallouts();
    }

}
