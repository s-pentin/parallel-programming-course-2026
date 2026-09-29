package solution.stage1;

import solution.BaseCollector;
import solution.Snapshot;

public class SyncThreadCollector extends BaseCollector {

    @Override
    public synchronized void record(long value) {
        recordUnsynchronized(value);
    }

    @Override
    public synchronized Snapshot snapshot() {
        return snapshotUnsynchronized();
    }
}
