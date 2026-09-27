package solution.stage1;

import solution.Benchmark;
import solution.Generator;
import solution.MetricsCollector;

public class Stage1EmptyMain {

    public static void main(String[] args) throws Exception {
        long[] values = Generator.generate();

        int[] threads = {1, 2, 4, 8, 12};

        System.out.println("\n=== Этап 1: пустой лок ===");
        for (int t : threads) {
            MetricsCollector c = new EmptyLockCollector();
            double ops = Benchmark.measurePoint(c, values, t);
            System.out.printf("T=%2d | %.2f млн ops/sec%n", t, ops / 1_000_000.0);
        }
    }
}
