package solution.stage0;

import solution.Benchmark;
import solution.Generator;
import solution.MetricsCollector;

public class Stage0Main {

    public static void main(String[] args) throws Exception {
        long[] values = Generator.generate();

        MetricsCollector collector = new SingleThreadCollector();

        double ops = Benchmark.measurePoint(collector, values, 1);
        System.out.printf("Этап 0 | 1 поток: %.2f млн ops/sec%n", ops / 1_000_000.0);
    }
}
