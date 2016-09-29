package nachos.kernel.threads;

import java.util.Comparator;

public class ScheduledRunnableComparator implements Comparator<ScheduledRunnable> {

    private static final class ScheduledRunnableComparatorInitializer {
	private static final ScheduledRunnableComparator INSTANCE = new ScheduledRunnableComparator();
    }
    
    @Override
    public int compare(final ScheduledRunnable o1, final ScheduledRunnable o2) {
	return Long.compare(o1.getScheduledTime(), o2.getScheduledTime());
    }

    public static ScheduledRunnableComparator getInstance() {
	return ScheduledRunnableComparatorInitializer.INSTANCE;
    }
}
