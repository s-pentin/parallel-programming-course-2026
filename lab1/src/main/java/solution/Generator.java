package solution;

import java.util.Random;

public class Generator {

    private static final int SEED = 17;
    private static final int MAX_K = 1023;
    private static final int SIZE = 1 << 20;
    private static final double ALPHA = 1.15;

    public static long[] generate() {
        double[] weights = new double[MAX_K + 1];
        double H = 0.0;
        for (int k = 1; k <= MAX_K; k++) {
            weights[k] = Math.pow(k, -ALPHA);
            H += weights[k];
        }

        double[] cdf = new double[MAX_K + 1];
        cdf[0] = 0.0;
        for (int k = 1; k <= MAX_K; k++) {
            cdf[k] = cdf[k - 1] + weights[k] / H;
        }
        cdf[MAX_K] = 1.0;

        Random rnd = new Random(SEED);
        long[] values = new long[SIZE];

        for (int i = 0; i < SIZE; i++) {
            double u = rnd.nextDouble();
            int lo = 1, hi = MAX_K;
            while (lo < hi) {
                int mid = (lo + hi) >>> 1;
                if (cdf[mid] < u) {
                    lo = mid + 1;
                } else {
                    hi = mid;
                }
            }
            values[i] = lo;
        }

        return values;
    }

//    public static void main(String[] args) {
//        long t0 = System.nanoTime();
//        long[] values = generate();
//        long t1 = System.nanoTime();
//
//        System.out.printf("Сгенерировано %d элементов за %.1f мс%n",
//                values.length, (t1 - t0) / 1e6);
//
//        // проверка распределения (корзины)
//        int[] hist = new int[MAX_K + 1];
//        for (int v : values) {
//            hist[v]++;
//        }
//
//        System.out.println("Доля в корзине 1 (самая частая): " +
//                String.format("%.3f", hist[1] / (double) SIZE));
//        System.out.println("Доля в корзинах 1..3: " +
//                String.format("%.3f",
//                        (hist[1] + hist[2] + hist[3]) / (double) SIZE));
//        System.out.println("Первые 10 значений: ");
//        for (int i = 0; i < 40; i++) {
//            System.out.print(values[i] + " ");
//        }
//        System.out.println();
//    }
}