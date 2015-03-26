package ru.ifmo.ctddev.itegulov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
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
    private final ParallelMapper parallelMapper;

    /**
     * Class constructor, specifying which {@link info.kgeorgiy.java.advanced.mapper.ParallelMapper}
     * to use.
     * @param parallelMapper parallel mapper, which will be used for parallel computations
     */
    public IterativeParallelism(ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
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
        return parallelizeList(count, pseudoMonoid, Function.identity(), list);
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
        return parallelizeList(count, pseudoMonoid,
                a -> predicate.test(a) ? Collections.singletonList(a) : Collections.emptyList(), list);
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

    public static void main(String[] args) throws InterruptedException {
        try (ParallelMapper parallel = new ParallelMapperImpl(8)) {
            Integer res = new IterativeParallelism(parallel).maximum(2, Arrays.asList(1, 2, 3), Integer::compare);
            System.out.println(res);
        }
    }

    private <T, E> E parallelizeList(int count,
                                     PseudoMonoid<E> pseudoMonoid,
                                     Function<? super T, ? extends E> caster,
                                     List<? extends T> list) throws InterruptedException {
        if (count > list.size()) {
            count = list.size();
        }
        List<List<? extends T>> tasks = new ArrayList<>();
        if (count == 1) {
            tasks.add(list);
        } else {
            int chunkSize = list.size() / count;
            for (int left = 0; left < list.size(); left += chunkSize) {
                int right = Math.min(left + chunkSize, list.size());
                tasks.add(list.subList(left, right));
            }
        }

        List<E> result = parallelMapper.map(t -> {
            E accumulator = pseudoMonoid.getNeutral();
            for (T element : t) {
                accumulator = pseudoMonoid.operation(accumulator, caster.apply(element));
            }
            return accumulator;
        }, tasks);

        E answer = result.get(0);
        for (int i = 1; i < result.size(); i++) {
            answer = pseudoMonoid.operation(answer, result.get(i));
        }

        return answer;
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
