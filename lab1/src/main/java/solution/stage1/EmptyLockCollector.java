package solution.stage1;

import solution.MetricsCollector;
import solution.Snapshot;

public class EmptyLockCollector implements MetricsCollector {

    @Override
    public synchronized void record(long value) {
        // намеренно пусто — только берём и отпускаем лок
    }

    @Override
    public synchronized Snapshot snapshot() {
        // возвращаем пустой снимок
        return new Snapshot(new long[256], 0, 0, 0, 0, 0, 0);
    }
}