package solution.stage0;

import solution.BaseCollector;
import solution.Snapshot;

public class SingleThreadCollector extends BaseCollector {

    @Override
    public void record(long value) {
        recordUnsynchronized(value);
    }

    @Override
    public Snapshot snapshot() {
        return snapshotUnsynchronized();
    }
}
