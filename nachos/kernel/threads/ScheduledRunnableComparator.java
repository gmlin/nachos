package nachos.kernel.threads;

import java.util.Comparator;

public class ScheduledRunnableComparator implements Comparator<ScheduledRunnable> {

    @Override
    public int compare(final ScheduledRunnable o1, final ScheduledRunnable o2) {
	return Long.compare(o1.getScheduledTime(), o2.getScheduledTime());
    }

}
