package lab2;

/* Этап 2: лок, о котором знает планировщик. Поток, ждущий занятый лок, не попадает в enabled;
 * обычный synchronized / ReentrantLock здесь нельзя — поток уснёт с ходом в руках и всё зависнет.
 * Лог: "lock <имя>", "unlock <имя>". Планировщик должен знать все локи, чтобы печатать held={a:A, b:B}. */
public final class SchedLock {
    final String name;

    /**
     * Создаётся в setup() тестовой программы; зарегистрируйте лок в планировщике s.
     */
    public SchedLock(Scheduler s, String name) {
        this.name = name;
        s.registerLock(this);
    }

    /**
     * Пауза; если лок занят — ждать, не участвуя в выборе; затем стать владельцем.
     */
    public void lock() {
        Scheduler scheduler = Scheduler.currentScheduler();
        if (scheduler == null) {
            throw new IllegalStateException("lock outside Scheduler.run()");
        }
        scheduler.lock(this);
    }

    /**
     * Пауза; освободить лок, ждавшие потоки снова могут быть выбраны.
     */
    public void unlock() {
        Scheduler scheduler = Scheduler.currentScheduler();
        if (scheduler == null) {
            throw new IllegalStateException("unlock outside Scheduler.run()");
        }
        scheduler.unlock(this);
    }
}
