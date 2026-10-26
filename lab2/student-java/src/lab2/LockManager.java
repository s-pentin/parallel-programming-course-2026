package lab2;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Этап 2: владельцы зарегистрированных SchedLock и снимок удерживаемых локов
 */
final class LockManager {
    private final Map<SchedLock, Integer> owners = new LinkedHashMap<>();

    synchronized void register(SchedLock lock) {
        owners.put(lock, null);
    }

    synchronized Integer owner(SchedLock lock) {
        return owners.get(lock);
    }

    synchronized void acquire(SchedLock lock, int threadId) {
        owners.put(lock, threadId);
    }

    synchronized void release(SchedLock lock, int threadId) {
        if (!Integer.valueOf(threadId).equals(owners.get(lock))) {
            throw new IllegalMonitorStateException("thread does not own lock " + lock.name);
        }
        owners.put(lock, null);
    }

    synchronized boolean hasHeldLocks() {
        return owners.values().stream().anyMatch(Objects::nonNull);
    }

    synchronized String held() {
        StringBuilder result = new StringBuilder("held={");
        boolean first = true;
        for (Map.Entry<SchedLock, Integer> entry : owners.entrySet()) {
            if (entry.getValue() == null) continue;
            if (!first) result.append(", ");
            result.append(entry.getKey().name).append(':')
                    .append((char) ('A' + entry.getValue()));
            first = false;
        }
        return result.append('}').toString();
    }
}
