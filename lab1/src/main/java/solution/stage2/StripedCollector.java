package solution.stage2;

import solution.MetricsCollector;
import solution.Snapshot;

import java.util.concurrent.atomic.AtomicLong;

public class StripedCollector implements MetricsCollector {

    private static final int STRIPES = 16;

    private final long[] buckets = new long[256];
    private final Object[] locks = new Object[STRIPES];

    private final AtomicLong count = new AtomicLong();
    private final AtomicLong sum = new AtomicLong();
    private final AtomicLong min = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong max = new AtomicLong(0);

    public StripedCollector() {
        for (int i = 0; i < STRIPES; i++) {
            locks[i] = new Object();
        }
    }

    @Override
    public void record(long value) {
        int bucket = (int) Math.min(value / 4, 255);
        int stripe = bucket % STRIPES;

        synchronized (locks[stripe]) {
            buckets[bucket]++;
        }

        count.incrementAndGet();
        sum.addAndGet(value);

        long currentMin;
        do {
            currentMin = min.get();
            if (value >= currentMin) {
                break;
            }
        } while (!min.compareAndSet(currentMin, value));

        long currentMax;
        do {
            currentMax = max.get();
            if (value <= currentMax) {
                break;
            }
        } while (!max.compareAndSet(currentMax, value));
    }

    @Override
    public Snapshot snapshot() {
        long[] copy = new long[256];

        for (int stripe = 0; stripe < STRIPES; stripe++) {
            synchronized (locks[stripe]) {
                for (int b = stripe; b < 256; b += STRIPES) {
                    copy[b] = buckets[b];
                }
            }
        }

        long cnt = count.get();
        long sm = sum.get();
        long mn = min.get();
        long mx = max.get();

        long p50 = percentile(copy, cnt, 0.50);
        long p99 = percentile(copy, cnt, 0.99);

        return new Snapshot(copy, cnt, sm, mn, mx, p50, p99);
    }
}
