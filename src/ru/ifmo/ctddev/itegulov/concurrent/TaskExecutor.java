package ru.ifmo.ctddev.itegulov.concurrent;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Function;

/**
 * Class, providing way to execute submitted {@link java.util.function.Function}.
 * This interface provides a way of decoupling task submission from the
 * mechanics of how each task will be executed, including details of thread
 * use, scheduling, etc. For example, rather than creating {@link java.lang.Thread}
 * for each of a set of tasks, you might use:
 *
 * <pre>
 * TaskExecutor executor = <em>anTaskExecutor</em>;
 * FutureTask fTask1 = executor.submit(task1, arg1);
 * FutureTask fTask2 = executor.submit(task2, arg2);
 * ...
 * </pre>
 * @author Daniyar Itegulov
 */
public class TaskExecutor {
    private final Queue<FutureTask> tasks = new LinkedList<>();
    private final Thread[] threads;

    /**
     * Class constructor, which specify how many threads can be used
     * for executing passed tasks
     * @param threads number of threads
     */
    public TaskExecutor(int threads) {
        this.threads= new Thread[threads];
        for (int i = 0; i < threads; i++) {
            this.threads[i] = new WorkerThread();
            this.threads[i].start();
        }
    }

    /**
     * Executes passed task asynchronously and returns future task,
     * which can be overviewed for the result of executing.
     *
     * @param task task, to be executed
     * @param argument argument, which will be passed to function
     * @return future task, which will contain result of executing
     * {@code task} with {@code argument} passed.
     */
    public <R, T> FutureTask<R, T> submit(Function<T, R> task, T argument) {
        FutureTask<R, T> futureTask = new FutureTask<>(task, argument);
        synchronized (tasks) {
            tasks.add(futureTask);
            tasks.notifyAll();
        }
        return futureTask;
    }

    /**
     * Shutdowns all threads, used for executing tasks and cancels all
     * future tasks, which were provided by {@link #submit}, but not executed
     * yet.
     */
    public void shutdown() {
        synchronized (tasks) {
            tasks.forEach(FutureTask::cancel);
            tasks.clear();
        }
        for (Thread thread : threads) {
            thread.interrupt();
        }
    }

    private class WorkerThread extends Thread {
        @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
        @Override
        public void run() {
            while (!isInterrupted()) {
                FutureTask futureTask;
                synchronized (tasks) {
                    while (tasks.isEmpty()) {
                        try {
                            tasks.wait();
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                    futureTask = tasks.poll();
                }
                futureTask.execute();
                synchronized (futureTask) {
                    futureTask.notifyAll();
                }
            }
        }
    }
}
