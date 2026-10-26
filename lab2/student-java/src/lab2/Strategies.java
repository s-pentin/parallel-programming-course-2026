package lab2;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/* Стратегии выбора следующего потока.
 * FirstAlive — этап 1, Replay — этап 2, RandomPick — этап 3, Dfs — этап 4 (ограничение bound — этап 5).
 * enabled — номера потоков, которые могут сделать шаг, по возрастанию; last — прошлый выбранный (-1 в начале). */
public final class Strategies {
    private Strategies() {
    }

    /**
     * Всегда первый поток из enabled.
     */
    public static final class FirstAlive implements Strategy {
        public int pick(List<Integer> enabled, int last) {
            return enabled.getFirst();
        }
    }

    /**
     * Проигрывает трассу: на i-м решении выбирает поток trace[i] ('A' -> 0, 'B' -> 1).
     */
    public static final class Replay implements Strategy {
        private final String trace;
        private int position;

        public Replay(String trace) {
            this.trace = trace;
        }

        /**
         * Сбросить позицию в трассе перед прогоном.
         */
        public void beginRun() {
            position = 0;
        }

        public int pick(List<Integer> enabled, int last) {
            if (position >= trace.length()) {
                throw new IllegalArgumentException("replay trace ended at decision " + position);
            }
            int id = trace.charAt(position++) - 'A';
            if (!enabled.contains(id)) {
                throw new IllegalArgumentException("replay selected disabled thread " + (char) ('A' + id));
            }
            return id;
        }
    }

    /**
     * Случайный поток: enabled.get(rnd.nextInt(enabled.size())), rnd = new java.util.Random(seed).
     */
    public static final class RandomPick implements Strategy {
        private final Random random;

        public RandomPick(long seed) {
            random = new Random(seed);
        }

        public int pick(List<Integer> enabled, int last) {
            return enabled.get(random.nextInt(enabled.size()));
        }
    }

    /**
     * Stateless DFS по дереву выборов: каждый прогон с нуля, повторяет префикс прошлого и берёт следующий вариант.
     * bound — максимум вытеснений (этап 5); для dfs runner передаёт Integer.MAX_VALUE.
     */
    public static final class Dfs implements Strategy {
        private final int bound;
        private final List<List<Integer>> options = new ArrayList<>();
        private final List<Integer> selected = new ArrayList<>();

        private int position;
        private int preemptions;

        public Dfs(int bound) {
            this.bound = bound;
        }

        /**
         * Подготовиться к новому прогону (позиция в пути, счётчик вытеснений).
         */
        public void beginRun() {
            position = 0;
            preemptions = 0;
        }

        public int pick(List<Integer> enabled, int last) {
            List<Integer> candidates = last >= 0 && enabled.contains(last) && preemptions >= bound
                    ? List.of(last)
                    : List.copyOf(enabled);

            int choice;
            if (position < selected.size()) {
                if (!options.get(position).equals(candidates)) {
                    throw new IllegalStateException("DFS path is not reproducible");
                }
                choice = selected.get(position);
            } else {
                options.add(candidates);
                choice = candidates.getFirst();
                selected.add(choice);
            }

            if (last >= 0 && enabled.contains(last) && choice != last) {
                preemptions++;
            }
            position++;
            return choice;
        }

        /**
         * Выбрать следующий путь; false — дерево обойдено.
         */
        public boolean endRun() {
            while (selected.size() > position) {
                selected.removeLast();
                options.removeLast();
            }

            for (int i = position - 1; i >= 0; i--) {
                List<Integer> candidates = options.get(i);
                int next = candidates.indexOf(selected.get(i)) + 1;
                if (next < candidates.size()) {
                    selected.set(i, candidates.get(next));
                    while (selected.size() > i + 1) {
                        selected.removeLast();
                        options.removeLast();
                    }
                    return true;
                }
            }
            return false;
        }
    }
}
