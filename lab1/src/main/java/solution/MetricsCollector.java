package solution;

public interface MetricsCollector {
    void record(long value);
    Snapshot snapshot();

    default long percentile(long[] buckets, long count, double percentile) {
        if (count == 0) {
            return 0;
        }

        long threshold = (long) (count * percentile);
        long accumulated = 0;

        for (int i = 0; i < 256; i++) {
            accumulated += buckets[i];
            if (accumulated >= threshold) {
                return i * 4L;
            }
        }
        return 255 * 4L;
    }
}