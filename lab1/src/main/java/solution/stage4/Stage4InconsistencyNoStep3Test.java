package solution.stage4;

import solution.Generator;
import solution.MetricsCollector;
import solution.Snapshot;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class Stage4InconsistencyNoStep3Test {
    public static void main(String[] args) throws Exception {
        long[] values = Generator.generate();
        MetricsCollector collector = new DoubleBufferCollectorNoStep3();

        int writers = 4;
        int snapshots = 10_000;

        AtomicBoolean stop = new AtomicBoolean(false);
        AtomicLong totalRecords = new AtomicLong();
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService pool = Executors.newFixedThreadPool(writers);
        for (int t = 0; t < writers; t++) {
            final int idx = t;
            pool.submit(() -> {
                long local = 0;
                int i = idx * 1000;
                try { start.await(); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                while (!stop.get()) {
                    collector.record(values[i]);
                    local++;
                    i = (i + 1) % values.length;
                }
                totalRecords.addAndGet(local);
            });
        }

        start.countDown();

        int broken = 0, sumLess = 0, sumGreater = 0;
        for (int s = 0; s < snapshots; s++) {
            Snapshot snap = collector.snapshot();
            long bucketSum = 0;
            for (long b : snap.buckets()) bucketSum += b;

            if (bucketSum != snap.count()) {
                broken++;
                if (bucketSum < snap.count()) sumLess++;
                else sumGreater++;
            }
        }

        stop.set(true);
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        long finalCount = collector.snapshot().count();
        long actual = totalRecords.get();

        System.out.println("=== Стресс-тест (Этап 4) Без Шага 3 ===");
        System.out.printf("Битых снимков: %d / %d (%.2f%%)%n", broken, snapshots, 100.0 * broken / snapshots);
        System.out.printf("  sum < count : %d%n", sumLess);
        System.out.printf("  sum > count : %d%n", sumGreater);
        System.out.printf("Итоговый count = %d%n", finalCount);
        System.out.printf("Реальных вызовов = %d%n", actual);
        System.out.printf("Разница = %d%n", finalCount - actual);
    }
}