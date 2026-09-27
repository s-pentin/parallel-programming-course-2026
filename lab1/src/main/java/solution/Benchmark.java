package solution;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Arrays;

public class Benchmark {

    public static double run(MetricsCollector collector, long[] values, int threadCount, int seconds) throws InterruptedException {
        CountDownLatch start = new CountDownLatch(1);
        AtomicBoolean stop = new AtomicBoolean(false);
        long[] ops = new long[threadCount];

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int k = 0; k < threadCount; k++) {
            final int threadIndex = k;
            executor.submit(() -> {
                long localCount = 0;
                int i = threadIndex * 1000;

                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                while (!stop.get()) {
                    collector.record(values[i]);
                    localCount++;
                    i++;
                    if (i == values.length) {
                        i = 0;
                    }
                }

                ops[threadIndex] = localCount;
            });
        }

        long t0 = System.nanoTime();
        start.countDown();

        Thread.sleep(seconds * 1000L);

        stop.set(true);
        long t1 = System.nanoTime();

        executor.shutdown();
        if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }

        long totalOps = 0;
        for (long op : ops) {
            totalOps += op;
        }

        double elapsedSec = (t1 - t0) / 1_000_000_000.0;
        return totalOps / elapsedSec;
    }

    /**
     * Замер одной точки графика (прогрев + 5 честных забегов).
     * Возвращает медиану.
     */
    public static double measurePoint(MetricsCollector collector, long[] values, int threadCount) throws InterruptedException {
        // прогрев 5 секунд
        run(collector, values, threadCount, 5);

        double[] results = new double[5];
        for (int i = 0; i < 5; i++) {
            results[i] = run(collector, values, threadCount, 5);
        }

        System.out.println("snapshot.count = " + collector.snapshot().count());

        Arrays.sort(results);
        return results[2];
    }
}