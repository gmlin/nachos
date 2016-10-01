package nachos.kernel.threads;

import java.util.Comparator;

public class ScheduledRunnableComparator implements Comparator<ScheduledRunnable> {

    private static class ScheduledRunnableComparatorInitializer {
	private static final ScheduledRunnableComparator INSTANCE = new ScheduledRunnableComparator();
    }
    
    @Override
    public int compare(ScheduledRunnable o1, ScheduledRunnable o2) {
	return Long.compare(o2.getScheduledTime(), o1.getScheduledTime());
    }

    public static ScheduledRunnableComparator getInstance() {
	return ScheduledRunnableComparatorInitializer.INSTANCE;
    }
}
