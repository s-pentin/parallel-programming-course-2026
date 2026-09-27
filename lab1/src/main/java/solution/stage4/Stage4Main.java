package solution.stage4;

import solution.Benchmark;
import solution.Generator;
import solution.MetricsCollector;

public class Stage4Main {
    public static void main(String[] args) throws Exception {
        long[] values = Generator.generate();
        int[] threads = {1, 2, 4, 8, 12};

        System.out.println("=== Этап 4: Double Buffering ===");
        for (int t : threads) {
            MetricsCollector c = new DoubleBufferCollector();
            double ops = Benchmark.measurePoint(c, values, t);
            System.out.printf("T=%2d | %.2f млн ops/sec%n", t, ops / 1_000_000.0);
        }
    }
}