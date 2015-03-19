package ru.ifmo.ctddev.itegulov.iterativeparallelism;

import info.kgeorgiy.java.advanced.concurrent.ListIP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Class, containing some useful parallel functions
 *
 * @author Daniyar Itegulov
 */
public class IterativeParallelism implements ListIP {
    /**
     * Object, used for synchronising
     */
    private final Object lock = new Object();

    @Override
    public <T> T maximum(int threads, List<? extends T> list, Comparator<? super T> comparator) throws InterruptedException {
        Monoid<T> monoid = new Monoid<>((a, b) -> (comparator.compare(a, b) >= 0) ? a : b, () -> list.get(0));
        return parallelizeList(threads, monoid, (a) -> a, list);
    }

    @Override
    public <E> E minimum(int threads, List<? extends E> list, Comparator<? super E> comparator) throws InterruptedException {
        Monoid<E> monoid = new Monoid<>((a, b) -> (comparator.compare(a, b) <= 0) ? a : b, () -> list.get(0));
        return parallelizeList(threads, monoid, (a) -> a, list);
    }

    @Override
    public <E> boolean any(int threads, List<? extends E> list, Predicate<? super E> predicate) throws InterruptedException {
        Monoid<Boolean> monoid = new Monoid<>(Boolean::logicalOr, () -> Boolean.FALSE);
        return parallelizeList(threads, monoid, predicate::test, list);
    }

    @Override
    public <E> boolean all(int threads, List<? extends E> list, Predicate<? super E> predicate) throws InterruptedException {
        Monoid<Boolean> monoid = new Monoid<>(Boolean::logicalAnd, () -> Boolean.TRUE);
        return parallelizeList(threads, monoid, predicate::test, list);
    }

    @Override
    public String concat(int threads, List<?> list) throws InterruptedException {
        Monoid<String> monoid = new Monoid<>(String::concat, () -> "");
        return parallelizeList(threads, monoid, (a) -> a.toString(), list);
    }

    @Override
    public <T> List<T> filter(int threads, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        Monoid<List<T>> monoid = new Monoid<>((a, b) -> {
            a.addAll(b);
            return a;
        }, ArrayList::new);
        return parallelizeList(threads, monoid, (a) -> {
            if (predicate.test(a)) {
                return Arrays.asList(a);
            } else {
                return new ArrayList<>();
            }
        }, list);
    }

    @Override
    public <T, U> List<U> map(int threads,
                              List<? extends T> list,
                              Function<? super T, ? extends U> function) throws InterruptedException {
        Monoid<List<U>> monoid = new Monoid<>((a, b) -> {
            a.addAll(b);
            return a;
        }, ArrayList::new);
        return parallelizeList(threads, monoid, (a) -> Arrays.asList(function.apply(a)), list);
    }

    /**
     * Uses <code>count</code> threads to cast all <code>list</code> elements using <code>caster</code>
     * and apply monoid's operation over them
     *
     * @param count number of thread to use
     * @param monoid monoid, containing neutral element and operation to use
     * @param caster function, used to cast <code>list</code> element to <code>monoid</code> element
     * @param list list, containing data to process
     * @param <T> type of elements in <code>list</code>
     * @param <E> resulting type
     * @return result of applying <code>monoid</code> operation over all elements in <code>list</code>
     * @throws InterruptedException if one of generated threads has interrupted
     * @see ru.ifmo.ctddev.itegulov.iterativeparallelism.IterativeParallelism.Monoid
     * @see java.util.function.Function
     */
    private <T, E> E parallelizeList(int count,
                                     Monoid<E> monoid,
                                     Function<T, E> caster,
                                     List<T> list) throws InterruptedException {
        int chunkSize = list.size() / count;
        List<Thread> threadList = new ArrayList<>();
        int index = 0;
        final Accumulator<E> result = new Accumulator<>(monoid.getNeutral(), 0);
        for (int left = 0; left < list.size(); left += chunkSize) {
            int right = Math.min(left + chunkSize, list.size());
            List<T> subList = list.subList(left, right);
            final int currentIndex = index;
            Thread thread = new Thread(() -> {
                E accumulator = monoid.getNeutral();
                for (T element : subList) {
                    accumulator = monoid.operation(accumulator, caster.apply(element));
                }

                synchronized (lock) {
                    while (result.getThreadIndex() != currentIndex) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    result.setValue(monoid.operation(result.getValue(), accumulator));
                    result.setThreadIndex(result.getThreadIndex() + 1);
                    lock.notifyAll();
                }
            });
            thread.start();
            threadList.add(thread);
            index++;
        }

        for (Thread thread : threadList) {
            thread.join();
        }

        return result.getValue();
    }

    /**
     * Represents current value, received from ended threads and next thread to write it's result
     *
     * @param <T> value type, returned by threads
     */
    private static class Accumulator<T> {
        /**
         * Current value, generated by ended threads
         */
        private T value;
        /**
         * Next thread to write it's result
         */
        private int threadIndex;

        /**
         * Class constructor, specifying initial value, when thread didn't return any result
         * (for example monoid's neutral element) and next thread's index
         *
         * @param value initial value
         * @param threadIndex next thread's index
         */
        private Accumulator(final T value, final int threadIndex) {
            this.value = value;
            this.threadIndex = threadIndex;
        }

        /**
         * @return current value
         * @see #value
         */
        private T getValue() {
            return value;
        }

        /**
         * Sets new value
         * @param value new value to set
         * @see #value
         */
        private void setValue(final T value) {
            this.value = value;
        }

        /**
         * @return current thread index
         * @see #threadIndex
         */
        private int getThreadIndex() {
            return threadIndex;
        }

        /**
         * Sets new thread index
         * @param threadIndex new thread index to set
         * @see #threadIndex
         */
        private void setThreadIndex(final int threadIndex) {
            this.threadIndex = threadIndex;
        }
    }

    /**
     * Represents algebraic structure monoid with a single associative operation binary operation
     * and a neutral element.
     * <p>
     * <a href="https://en.wikipedia.org/wiki/Monoid">Monoid on wikipedia</a>
     *
     * @param <T> type of elements in monoid
     */
    private static class Monoid<T> {
        /**
         * Associative operation, used by monoid. It can overwrite it's arguments
         */
        private final BinaryOperator<T> operation;
        /**
         * Supplier, which generate neutral element. It has to give out different objects
         * (<code>neutralElementGenerator.get() != neutralElementGenerator.get()</code>.
         */
        private final Supplier<T> neutralElementGenerator;

        /**
         * Class constructor, specifying what operation is to be used in monoid and how neutral
         * element is generated
         *
         * @param operation binary operation, which specifies monoid's operation
         * @param neutralElementGenerator supplier, which specifies monoid's neutral element generation
         */
        private Monoid(final BinaryOperator<T> operation, final Supplier<T> neutralElementGenerator) {
            this.operation = operation;
            this.neutralElementGenerator = neutralElementGenerator;
        }

        /**
         * @param a first element of monoid
         * @param b second element of monoid
         * @return result of operation on <code>a</code> and <code>b</code>
         * @see #operation
         */
        public T operation(T a, T b) {
            return operation.apply(a, b);
        }

        /**
         * @return neutral element of monoid
         * @see #neutralElementGenerator
         */
        public T getNeutral() {
            return neutralElementGenerator.get();
        }
    }
}
