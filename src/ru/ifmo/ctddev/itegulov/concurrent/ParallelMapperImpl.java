package ru.ifmo.ctddev.itegulov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Basic implementation of {@link info.kgeorgiy.java.advanced.mapper.ParallelMapper}
 *
 * @author Daniyar Itegulov
 */
public class ParallelMapperImpl implements ParallelMapper {
    private TaskExecutor taskExecutor;

    /**
     * Class constructor, specifying what number of threads to use
     * for parallel computation
     *
     * @param threads number of threads to use
     */
    public ParallelMapperImpl(int threads) {
        taskExecutor = new TaskExecutor(threads);
    }

    @Override
    public <T, R> List<R> map(final Function<? super T, ? extends R> function, final List<? extends T> list) throws InterruptedException {
        List<FutureTask<R, T>> futureTasks = new ArrayList<>();
        for (T arg : list) {
            futureTasks.add(taskExecutor.submit(function::apply, arg));
        }
        List<R> result = new ArrayList<>();
        for (FutureTask<R, T> futureTask : futureTasks) {
            futureTask.waitForDone();
            if (futureTask.isReady()) {
                result.add(futureTask.getResult());
            }
        }
        return result;
    }

    @Override
    public void close() throws InterruptedException {
        taskExecutor.shutdown();
    }

    public static void main(String[] args) throws InterruptedException {
        try (ParallelMapper parallelMapper = new ParallelMapperImpl(10)) {
            List<Integer> list = parallelMapper.map((a) -> a + 1, Arrays.asList(1, 2, 3, 4));
            System.out.println(list);
        }
    }
}
