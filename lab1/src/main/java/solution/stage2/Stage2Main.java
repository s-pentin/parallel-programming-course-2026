package solution.stage2;

import solution.Benchmark;
import solution.Generator;
import solution.MetricsCollector;

public class Stage2Main {

    public static void main(String[] args) throws Exception {
        long[] values = Generator.generate();
        int[] threads = {1, 2, 4, 8, 12};

        System.out.println("=== Этап 2: Lock Striping (16 групп) ===");
        for (int t : threads) {
            MetricsCollector c = new StripedCollector();
            double ops = Benchmark.measurePoint(c, values, t);
            System.out.printf("T=%2d | %.2f млн ops/sec%n", t, ops / 1_000_000.0);
        }
    }
}