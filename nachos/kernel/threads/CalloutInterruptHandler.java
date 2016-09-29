package nachos.kernel.threads;

import nachos.machine.InterruptHandler;
import nachos.machine.Timer;

public class CalloutInterruptHandler implements InterruptHandler {

    private final Timer timer;
    private final Callout callout;
    
    public CalloutInterruptHandler(final Timer timer, final Callout callout) {
	this.timer = timer;
	this.callout = callout;
    }
    
    @Override
    public void handleInterrupt() {
	callout.updateCurrentTime();
	callout.performCallouts();
    }

}
