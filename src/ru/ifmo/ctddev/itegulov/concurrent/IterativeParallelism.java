package ru.ifmo.ctddev.itegulov.concurrent;

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
 * Implementation of interface {@link info.kgeorgiy.java.advanced.concurrent.ListIP}
 *
 * @author Daniyar Itegulov
 */
public class IterativeParallelism implements ListIP {
    private final Object lock = new Object();

    /**
     * Default constructor
     */
    public IterativeParallelism() {
    }

    /**
     * Returns the maximum element of {@code List} according to the provided {@link java.util.Comparator}.
     * Uses {@code count} threads to do this parallel.
     * @param count number of threads to use
     * @param list list to process
     * @param comparator comparator to use
     * @return maximum element of {@code List}
     * @throws InterruptedException if some of created threads was interrupted
     */
    @Override
    public <E> E maximum(int count, List<? extends E> list, Comparator<? super E> comparator) throws InterruptedException {
        if (list.size() < 1) {
            throw new IllegalArgumentException("Can't find maximum of empty list");
        }
        PseudoMonoid<E> pseudoMonoid = new PseudoMonoid<>((a, b) -> (comparator.compare(a, b) >= 0) ? a : b, () -> list.get(0));
        return parallelizeList(count, pseudoMonoid, (a) -> a, list);
    }

    /**
     * Returns the minimum element of {@code List} according to the provided {@link java.util.Comparator}.
     * Uses {@code count} threads to do this parallel.
     * @param count number of threads to use
     * @param list list to process
     * @param comparator comparator to use
     * @return minimum element of {@code List}
     * @throws InterruptedException if some of created threads was interrupted
     */
    @Override
    public <E> E minimum(int count, List<? extends E> list, Comparator<? super E> comparator) throws InterruptedException {
        if (list.size() < 1) {
            throw new IllegalArgumentException("Can't find minimum of empty list");
        }
        PseudoMonoid<E> pseudoMonoid = new PseudoMonoid<>((a, b) -> (comparator.compare(a, b) <= 0) ? a : b, () -> list.get(0));
        return parallelizeList(count, pseudoMonoid, (a) -> a, list);
    }

    /**
     * Returns whether any elements of {@code List} match the provided {@link java.util.function.Predicate}.
     * Returns {@code false} if {@code List} is empty. Uses {@code count} threads to do
     * this parallel.
     * @param count number of threads to use
     * @param list list to process
     * @param predicate predicate to apply to elements
     * @return {@code true} if any elements of {@code List} match the provided
     * predicate, otherwise {@code false}
     * @throws InterruptedException if some of created threads was interrupted
     */
    @Override
    public <E> boolean any(int count, List<? extends E> list, Predicate<? super E> predicate) throws InterruptedException {
        PseudoMonoid<Boolean> pseudoMonoid = new PseudoMonoid<>(Boolean::logicalOr, () -> Boolean.FALSE);
        return parallelizeList(count, pseudoMonoid, predicate::test, list);
    }

    /**
     * Returns whether all elements of {@code List} match the provided {@link java.util.function.Predicate}.
     * Returns {@code true} if {@code List} is empty. Uses {@code count} threads
     * to do this parallel.
     * @param count number of threads to use
     * @param list list to process
     * @param predicate predicate to apply to elements
     * @return {@code true} if either all elements of {@code List} match the
     * provided predicate or {@code List} is empty, otherwise {@code false}
     * @throws InterruptedException if some of created threads was interrupted
     */
    @Override
    public <E> boolean all(int count, List<? extends E> list, Predicate<? super E> predicate) throws InterruptedException {
        PseudoMonoid<Boolean> pseudoMonoid = new PseudoMonoid<>(Boolean::logicalAnd, () -> Boolean.TRUE);
        return parallelizeList(count, pseudoMonoid, predicate::test, list);
    }

    /**
     * Returns concatenated string representation of all elements from {@code List}.
     * Returns empty string if {@code List} is empty. Uses {@code count} threads to
     * do this parallel.
     * @param count number of threads to use
     * @param list list to process
     * @return string, containing concatenated string representations of all elements
     * from {@code List}
     * @throws InterruptedException if some of created threads was interrupted
     */
    @Override
    public String concat(int count, List<?> list) throws InterruptedException {
        PseudoMonoid<StringBuilder> pseudoMonoid = new PseudoMonoid<>(StringBuilder::append, StringBuilder::new);
        return this.parallelizeList(count, pseudoMonoid, o -> new StringBuilder(o.toString()), list).toString();
    }

    /**
     * Returns a {@code List} consisting of the elements of given {@code List} that match
     * the given {@link java.util.function.Predicate}. Uses {@code count} threads to do
     * this parallel.
     * @param count number of threads to use
     * @param list list to process
     * @param predicate predicate to apply to elements
     * @return the new {@code List}
     * @throws InterruptedException if some of created threads was interrupted
     */
    @Override
    public <T> List<T> filter(int count, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        PseudoMonoid<List<T>> pseudoMonoid = new PseudoMonoid<>((a, b) -> {
            a.addAll(b);
            return a;
        }, ArrayList::new);
        return parallelizeList(count, pseudoMonoid, (a) -> {
            if (predicate.test(a)) {
                return Arrays.asList(a);
            } else {
                return new ArrayList<>();
            }
        }, list);
    }

    /**
     * Returns a {@code List} consisting of the results of applying the given
     * {@link java.util.function.Function} to the elements of this {@code List}.
     * Uses {@code count} threads to do this parallel.
     * @param count number of threads to use
     * @param list list to process
     * @param function function to apply to elements
     * @return the new {@code List}
     * @throws InterruptedException if some of created threads was interrupted
     */
    @Override
    public <T, U> List<U> map(int count,
                              List<? extends T> list,
                              Function<? super T, ? extends U> function) throws InterruptedException {
        PseudoMonoid<List<U>> pseudoMonoid = new PseudoMonoid<>((a, b) -> {
            a.addAll(b);
            return a;
        }, ArrayList::new);
        return parallelizeList(count, pseudoMonoid, (a) -> Arrays.asList(function.apply(a)), list);
    }

    private <T, E> E parallelizeList(int count,
                                     PseudoMonoid<E> pseudoMonoid,
                                     Function<? super T, ? extends E> caster,
                                     List<? extends T> list) throws InterruptedException {
        if (count > list.size()) {
            count = list.size();
        }
        int chunkSize = list.size() / count;
        List<Thread> threadList = new ArrayList<>();
        int index = 0;
        final Accumulator<E> result = new Accumulator<>(pseudoMonoid.getNeutral(), 0);
        for (int left = 0; left < list.size(); left += chunkSize) {
            int right = Math.min(left + chunkSize, list.size());
            List<? extends T> subList = list.subList(left, right);
            final int currentIndex = index;
            Thread thread = new Thread(() -> {
                E accumulator = pseudoMonoid.getNeutral();
                for (T element : subList) {
                    accumulator = pseudoMonoid.operation(accumulator, caster.apply(element));
                }

                synchronized (lock) {
                    while (result.threadIndex != currentIndex) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                    result.value = pseudoMonoid.operation(result.value, accumulator);
                    result.threadIndex++;
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

        return result.value;
    }

    private static class Accumulator<T> {
        T value;
        int threadIndex;

        private Accumulator(final T value, final int threadIndex) {
            this.value = value;
            this.threadIndex = threadIndex;
        }
    }

    private static class PseudoMonoid<T> {
        private final BinaryOperator<T> operation;
        private final Supplier<T> neutralElementGenerator;

        private PseudoMonoid(final BinaryOperator<T> operation, final Supplier<T> neutralElementGenerator) {
            this.operation = operation;
            this.neutralElementGenerator = neutralElementGenerator;
        }

        public T operation(T a, T b) {
            return operation.apply(a, b);
        }

        public T getNeutral() {
            return neutralElementGenerator.get();
        }
    }
}
