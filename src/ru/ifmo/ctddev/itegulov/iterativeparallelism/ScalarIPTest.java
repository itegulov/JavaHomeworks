package ru.ifmo.ctddev.itegulov.iterativeparallelism;

import info.kgeorgiy.java.advanced.concurrent.ScalarIP;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Georgiy Korneev (kgeorgiy@kgeorgiy.info)
 */
@RunWith(JUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ScalarIPTest<P extends ScalarIP> {
    public static final int N = 10_000;
    private final Random random = new Random(3257083275083275083L);

    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(final Description description) {
            System.err.println("=== Running " + description.getMethodName());
        }
    };

    @Test
    public void test01_maximum() throws InterruptedException {
        test(N, Collections::max, ScalarIP::maximum, comparators);
    }

    @Test
    public void test02_minimum() throws InterruptedException {
        test(N, Collections::min, ScalarIP::minimum, comparators);
    }

    @Test
    public void test03_all() throws InterruptedException {
        test(N, (data, predicate) -> data.stream().allMatch(predicate), ScalarIP::all, predicates);
    }

    @Test
    public void test04_any() throws InterruptedException {
        test(N, (data, predicate) -> data.stream().anyMatch(predicate), ScalarIP::any, predicates);
    }

    @Test
    public void test05_sleepPerformance() throws InterruptedException {
        final List<Integer> data = randomList(200);
        final Comparator<Integer> sleepComparator = (o1, o2) -> {
            try {
                Thread.sleep(10);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
            return Integer.compare(o1, o2);
        };
        final double speedup = speedup(data, sleepComparator, 8);
        Assert.assertTrue("Not parallel", speedup > 5);
    }

    @Test
    public void test06_burnPerformance() throws InterruptedException {
        final List<Integer> data = randomList(200);
        final Comparator<Integer> burnComparator = (o1, o2) -> {
            int total = o1 + o2;
            for (int i = 0; i < 100_000_000; i++) {
                total += i;
            }
            if (total == o1 + o2) {
                throw new AssertionError();
            }
            return Integer.compare(o1, o2);
        };
        final int procs = Runtime.getRuntime().availableProcessors();
        final double speedup = speedup(data, burnComparator, procs * 2);
        Assert.assertTrue("Not parallel", speedup > procs / 1.5);
        Assert.assertTrue("Too parallel", speedup < procs * 1.1);
    }

    private double speedup(final List<Integer> data, final Comparator<Integer> sleepComparator, final int threads) throws InterruptedException {
        final long time1 = maximum(1, data, ScalarIP::maximum, sleepComparator);
        final long time2 = maximum(threads, data, ScalarIP::maximum, sleepComparator);
        final double speedup = time1 / (double) time2;
        System.err.format("Speed up %.1f\n", speedup);
        return speedup;
    }

    private long maximum(final int threads, final List<Integer> data, final ConcurrentFunction<P, Integer, Comparator<Integer>> f, final Comparator<Integer> comparator) throws InterruptedException {
        final long start = System.nanoTime();
        f.apply(createInstance(), threads, data, comparator);
        return System.nanoTime() - start;
    }

    protected <T, U> void test(final int n, final BiFunction<List<Integer>, U, T> fExpected, final ConcurrentFunction<P, T, U> fActual, final List<Named<U>> cases) throws InterruptedException {
        final P instance = createInstance();
        final List<Integer> data = randomList(n);
        for (final Named<U> named : cases) {
            final T expected = fExpected.apply(data, named.value);
            System.err.print(named.name + ", threads: ");
            for (int threads = 1; threads <= 10; threads++) {
                System.err.print(" " + threads);
                Assert.assertEquals(threads + " threads", expected, fActual.apply(instance, threads, data, named.value));
            }
            System.err.println();
        }
        System.err.println();
    }

    interface ConcurrentFunction<P, T, U> {
        T apply(P instance, int threads, List<Integer> data, U value) throws InterruptedException;
    }

    private List<Integer> randomList(final int size) {
        final List<Integer> pool = random.ints(Math.min(size, 1000_000)).boxed().collect(Collectors.toList());
        final List<Integer> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            result.add(pool.get(random.nextInt(pool.size())));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private P createInstance() {
        return (P) new IterativeParallelism();
    }

    protected final class Named<T> {
        public final String name;
        public final T value;

        public Named(final String name, final T value) {
            this.name = name;
            this.value = value;
        }
    }

    protected final List<Named<Comparator<Integer>>> comparators = Arrays.asList(
            new Named<>("Natural order", Integer::compare),
            new Named<>("Reverse order", (l1, l2) -> Integer.compare(l2, l1)),
            new Named<>("Div 100", Comparator.<Integer>comparingInt(v -> v / 100)),
            new Named<>("Even first", Comparator.<Integer>comparingInt(v -> v % 2).thenComparing(v -> v)),
            new Named<>("All equal", (v1, v2) -> 0)
    );

    protected final List<Named<Predicate<Integer>>> predicates = Arrays.asList(
            new Named<>("Equal 0", Predicate.isEqual(0)),
            new Named<>("Greater than 0", i -> i > 0),
            new Named<>("Even", i -> i % 2 == 0),
            new Named<>("True", i -> true),
            new Named<>("False", i -> false)
    );
}