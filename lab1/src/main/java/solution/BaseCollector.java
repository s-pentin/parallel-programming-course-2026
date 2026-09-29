package solution;

public abstract class BaseCollector implements MetricsCollector {

    protected final long[] buckets = new long[256];
    protected long count;
    protected long sum;
    protected long min = Long.MAX_VALUE;
    protected long max = 0;

    protected void recordUnsynchronized(long value) {
        int bucket = (int) Math.min(value / 4, 255);
        buckets[bucket]++;
        count++;
        sum += value;
        if (value < min) min = value;
        if (value > max) max = value;
    }

    protected Snapshot snapshotUnsynchronized() {
        long[] copy = buckets.clone();
        long p50 = percentile(copy, count, 0.50);
        long p99 = percentile(copy, count, 0.99);
        return new Snapshot(copy, count, sum, min, max, p50, p99);
    }
}