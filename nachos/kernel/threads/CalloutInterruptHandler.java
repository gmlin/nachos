package nachos.kernel.threads;

import nachos.machine.InterruptHandler;

public class CalloutInterruptHandler implements InterruptHandler {

    private final Callout callout;
    
    public CalloutInterruptHandler(final Callout callout) {
	this.callout = callout;
    }
    
    @Override
    public void handleInterrupt() {
	
    }

}
