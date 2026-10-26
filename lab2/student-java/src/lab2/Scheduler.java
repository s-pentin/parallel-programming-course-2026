package lab2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Semaphore;
import java.util.function.BooleanSupplier;

/* Этап 1: планировщик, который ведёт настоящие потоки строго по одному (передача хода через семафоры).
 * Этап 2: обнаружение deadlock в главном цикле и корректное завершение застрявших потоков.
 * Внутреннее устройство (свои классы, семафоры, вспомогательные методы для Shared и SchedLock) придумываете сами. */
public final class Scheduler {
    /**
     * Если true, каждая операция с общими данными добавляет строку в log. Выставляет runner.
     */
    boolean logging;
    /**
     * Лог прогона: строки "<поток>  <операция>", например "A  lock mu", "A  balance -> 100".
     */
    final List<String> log = new ArrayList<>();
    private final List<Character> trace = new ArrayList<>();
    /**
     * null, если deadlock не было; иначе "held={a:A, b:B}" (этап 2).
     */
    String deadlock;

    private final List<Runnable> tasks = new ArrayList<>();
    private final List<Thread> threads = new ArrayList<>();
    private final List<Semaphore> permits = new ArrayList<>();
    private final List<Semaphore> signals = new ArrayList<>();
    private final LockManager locks = new LockManager();
    private SchedLock[] waitingLocks;
    private static final ThreadLocal<Integer> currentId = new ThreadLocal<>();
    private static final ThreadLocal<Scheduler> currentScheduler = new ThreadLocal<>();
    private boolean[] ready;
    private boolean[] finished;
    private boolean[] waiting;
    private BooleanSupplier[] waitConditions;
    private volatile boolean aborted;

    public Scheduler() {
    }

    /**
     * Зарегистрировать поток. Потоки получают буквы A, B, C… в порядке вызова spawn.
     */
    public void spawn(Runnable body) {
        tasks.add(body);
    }

    /**
     * Запустить потоки и вести их по одному: на каждой паузе спросить strategy.pick(enabled, last).
     */
    public void run(Strategy strategy) {
        int tasksSize = tasks.size();
        waitingLocks = new SchedLock[tasksSize];
        ready = new boolean[tasksSize];
        finished = new boolean[tasksSize];
        waiting = new boolean[tasksSize];
        waitConditions = new BooleanSupplier[tasksSize];
        boolean[] signalPending = new boolean[tasksSize];

        for (int i = 0; i < tasksSize; i++) {
            Semaphore permit = new Semaphore(0);
            permits.add(permit);
            signals.add(new Semaphore(0));
            ready[i] = true;

            Thread thread = getThread(i, permit);

            threads.add(thread);
            permit.release();
            thread.start();
        }

        // Every worker reaches its first scheduling point before the first choice.
        for (int i = 0; i < tasksSize; i++) {
            try {
                signals.get(i).acquire();
                signals.get(i).release();
                signalPending[i] = true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        int last = -1;
        while (true) {
            List<Integer> enabled = new ArrayList<>();
            synchronized (this) {
                boolean allFinished = true;
                for (int i = 0; i < tasksSize; i++) {
                    if (!finished[i]) {
                        allFinished = false;
                        if (waiting[i] && waitConditions[i].getAsBoolean()) {
                            waiting[i] = false;
                            ready[i] = true;
                        }
                        if (ready[i] && (waitingLocks[i] == null || locks.owner(waitingLocks[i]) == null)) {
                            enabled.add(i);
                        }
                    }
                }
                if (allFinished) break;
            }
            if (enabled.isEmpty()) {
                deadlock = locks.held();
                aborted = true;
                for (Thread thread : threads) {
                    thread.interrupt();
                }
                break;
            }
            int next;
            try {
                next = strategy.pick(enabled, last);
            } catch (IllegalArgumentException e) {
                aborted = true;
                for (Thread thread : threads) {
                    thread.interrupt();
                }
                for (Thread thread : threads) {
                    try {
                        thread.join(1000);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                throw e;
            }
            if (!enabled.contains(next)) {
                throw new IllegalArgumentException("strategy selected disabled thread " + next);
            }
            trace.add((char) ('A' + next));
            if (signalPending[next]) {
                try {
                    signals.get(next).acquire();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                signalPending[next] = false;
            }
            synchronized (this) {
                ready[next] = false;
            }
            permits.get(next).release();
            try {
                signals.get(next).acquire();
                signalPending[next] = false;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            last = next;
        }
        for (Thread thread : threads) {
            try {
                if (deadlock != null) {
                    thread.interrupt();
                    thread.join(1000);
                } else {
                    thread.join();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private Thread getThread(int i, Semaphore permit) {
        Runnable body = tasks.get(i);

        return new Thread(() -> {
            currentScheduler.set(this);
            currentId.set(i);

            try {
                permit.acquire();
                body.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (CancellationException ignored) {
                // Deadlock termination is already reported by the scheduler.
            } finally {
                synchronized (this) {
                    finished[i] = true;
                }
                signals.get(i).release();
                currentId.remove();
                currentScheduler.remove();
            }
        });
    }

    /**
     * Трасса прогона: буквы потоков, выбранных во всех решениях подряд, например "AAAABB".
     */
    public String traceString() {
        StringBuilder result = new StringBuilder(trace.size());
        trace.forEach(result::append);
        return result.toString();
    }

    /**
     * window() из тестовых программ: пауза перед следующей операцией с общими данными обязательна.
     */
    public static void point() {
        Scheduler scheduler = currentScheduler.get();
        Integer id = currentId.get();
        if (scheduler == null || id == null) {
            throw new IllegalStateException("Scheduler.point() called outside Scheduler.run()");
        }
        scheduler.recordOperation(id, "window");
    }

    static void beforeShared() {
        Scheduler scheduler = currentScheduler.get();
        Integer id = currentId.get();
        if (scheduler == null || id == null) {
            throw new IllegalStateException("shared access outside Scheduler.run()");
        }
        scheduler.reportPoint(id);
    }

    private void reportPoint(int id) {
        if (aborted) {
            throw new CancellationException();
        }
        synchronized (this) {
            if (aborted) {
                throw new CancellationException();
            }
            ready[id] = true;
        }
        signalAndWait(id);
    }

    private void signalAndWait(int id) {
        signals.get(id).release();
        try {
            permits.get(id).acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CancellationException();
        }
        if (aborted) {
            throw new CancellationException();
        }
    }

    static Scheduler currentScheduler() {
        return currentScheduler.get();
    }

    void recordShared(String name, String operation) {
        if (logging) {
            Integer id = currentId.get();
            char thread = id == null ? '?' : (char) ('A' + id);
            log.add(thread + "  " + name + " " + operation);
        }
    }

    private void recordOperation(int id, String operation) {
        if (logging) {
            log.add((char) ('A' + id) + "  " + operation);
        }
    }

    /**
     * Ждать, пока cond не станет true. До этого поток не попадает в enabled.
     */
    public static void await(BooleanSupplier cond) {
        Scheduler scheduler = currentScheduler.get();
        Integer id = currentId.get();
        if (scheduler == null || id == null) {
            throw new IllegalStateException("Scheduler.await() called outside Scheduler.run()");
        }
        if (cond.getAsBoolean()) {
            scheduler.reportPoint(id);
        }
        while (!cond.getAsBoolean()) {
            synchronized (scheduler) {
                scheduler.waiting[id] = true;
                scheduler.waitConditions[id] = cond;
                scheduler.ready[id] = false;
            }
            scheduler.signalAndWait(id);
        }
        synchronized (scheduler) {
            scheduler.waiting[id] = false;
            scheduler.waitConditions[id] = null;
        }
        scheduler.recordOperation(id, "await done");
    }

    void registerLock(SchedLock lock) {
        synchronized (this) {
            locks.register(lock);
        }
    }

    void lock(SchedLock lock) {
        Integer id = currentId.get();
        if (id == null) {
            throw new IllegalStateException("lock outside Scheduler.run()");
        }
        synchronized (this) {
            waitingLocks[id] = lock;
        }
        reportPoint(id);
        synchronized (this) {
            if (aborted) {
                throw new CancellationException();
            }
            locks.acquire(lock, id);
            waitingLocks[id] = null;
            recordOperation(id, "lock " + lock.name);
        }
    }

    void unlock(SchedLock lock) {
        Integer id = currentId.get();
        if (id == null) {
            throw new IllegalStateException("unlock outside Scheduler.run()");
        }
        if (aborted) {
            return;
        }
        reportPoint(id);
        synchronized (this) {
            if (aborted) {
                return;
            }
            locks.release(lock, id);
            recordOperation(id, "unlock " + lock.name);
        }
    }
}
