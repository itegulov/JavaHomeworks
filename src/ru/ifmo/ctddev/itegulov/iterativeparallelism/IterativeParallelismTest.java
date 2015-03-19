package ru.ifmo.ctddev.itegulov.iterativeparallelism;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.function.Predicate;

public class IterativeParallelismTest {
    private static final IterativeParallelism instance = new IterativeParallelism();
    private final Random random = new Random();

    @Test
    public void test1_simpleMaximum() throws Exception {
        testMaximum(2, Integer::compare, 2, 4, 1, 7, 18, 25, 2, 7);
        testMaximum(6, Integer::compare, 2, 4, 1, 7, 18, 25, 2, 7);
        testMaximum(5, Integer::compare, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20);
        testMaximum(1, Integer::compare, randomArray(10000));
        testMaximum(6, Integer::compare, randomArray(5));
    }

    @Test
    public void test2_simpleMinimum() throws Exception {
        testMinimum(2, Integer::compare, 4, 5, 1, 2, 7, 2, 3, 5, 10);
    }

    @Test
    public void test3_simpleAny() throws Exception {
        testAny(2, (a) -> a % 2 == 0, 1, 3, 4, 7, 9);
        testAny(2, (a) -> a % 2 == 0, 1, 3, 5, 7, 9);
    }

    @Test
    public void test4_simpleAll() throws Exception {
        testAll(2, (a) -> a % 2 == 0, 2, 4, 8, 10);
        testAll(2, (a) -> a % 2 == 0, 2, 4, 1, 8, 16);
    }

    public <T> void testMaximum(int threads, Comparator<? super T> comparator, T... elements) throws InterruptedException {
        T max = instance.maximum(threads, Arrays.asList(elements), comparator);
        Assert.assertEquals("Invalid maximum ", Arrays.stream(elements).max(comparator).get(), max);
    }

    public <T> void testMinimum(int threads, Comparator<? super T> comparator, T... elements) throws InterruptedException {
        T min = instance.minimum(threads, Arrays.asList(elements), comparator);
        Assert.assertEquals("Invalid minimum ", Arrays.stream(elements).min(comparator).get(), min);
    }

    public <T> void testAny(int threads, Predicate<? super T> predicate, T... elements) throws InterruptedException {
        boolean any = instance.any(threads, Arrays.asList(elements), predicate);
        Assert.assertEquals("Invalid any ", Arrays.stream(elements).anyMatch(predicate), any);
    }

    public <T> void testAll(int threads, Predicate<? super T> predicate, T... elements) throws InterruptedException {
        boolean all = instance.all(threads, Arrays.asList(elements), predicate);
        Assert.assertEquals("Invalid all ", Arrays.stream(elements).allMatch(predicate), all);
    }

    private Integer[] randomArray(final int size) {
        Integer[] array = new Integer[size];
        for (int i = 0; i < size; i++) {
            array[i] = random.nextInt();
        }
        return array;
    }
}