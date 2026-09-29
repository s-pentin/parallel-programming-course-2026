package solution.stage4;

import solution.MetricsCollector;
import solution.Snapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DoubleBufferCollector implements MetricsCollector {

    private static final int NOWHERE = -1;

    static final class ThreadBuffers {
        final long[][] buckets = new long[2][256];
        final long[] count = new long[2];
        final long[] sum = new long[2];
        final long[] min = {Long.MAX_VALUE, Long.MAX_VALUE};
        final long[] max = {0, 0};

        final AtomicInteger inside = new AtomicInteger(NOWHERE);
    }

    private volatile int active = 0;

    private final long[] globalBuckets = new long[256];
    private long globalCount = 0;
    private long globalSum = 0;
    private long globalMin = Long.MAX_VALUE;
    private long globalMax = 0;

    private final List<ThreadBuffers> allStates = new ArrayList<>();
    private final Object snapLock = new Object();

    private final ThreadLocal<ThreadBuffers> myState = ThreadLocal.withInitial(() -> {
        ThreadBuffers s = new ThreadBuffers();
        synchronized (snapLock) {
            allStates.add(s);
        }
        return s;
    });

    @Override
    public void record(long value) {
        ThreadBuffers my = myState.get();

        int b;
        while (true) {
            b = active;
            my.inside.set(b);

            if (active == b) {
                break;
            }
            my.inside.set(NOWHERE);
        }

        int bucket = (int) Math.min(value / 4, 255);
        my.buckets[b][bucket]++;
        my.count[b]++;
        my.sum[b] += value;
        if (value < my.min[b]) {
            my.min[b] = value;
        }
        if (value > my.max[b]) {
            my.max[b] = value;
        }

        my.inside.setRelease(NOWHERE);
    }

    @Override
    public Snapshot snapshot() {
        synchronized (snapLock) {
            int old = active;
            active = 1 - old;

            for (ThreadBuffers s : allStates) {
                while (s.inside.get() == old) {
                    Thread.onSpinWait();
                }
            }

            for (ThreadBuffers s : allStates) {
                globalCount += s.count[old];
                globalSum += s.sum[old];

                for (int i = 0; i < 256; i++) {
                    globalBuckets[i] += s.buckets[old][i];
                }

                if (s.min[old] < globalMin) {
                    globalMin = s.min[old];
                }
                if (s.max[old] > globalMax) {
                    globalMax = s.max[old];
                }

                s.count[old] = 0;
                s.sum[old] = 0;
                s.min[old] = Long.MAX_VALUE;
                s.max[old] = 0;
                Arrays.fill(s.buckets[old], 0);
            }

            long[] copy = globalBuckets.clone();
            long p50 = percentile(copy, globalCount, 0.50);
            long p99 = percentile(copy, globalCount, 0.99);

            return new Snapshot(copy, globalCount, globalSum, globalMin, globalMax, p50, p99);
        }
    }
}