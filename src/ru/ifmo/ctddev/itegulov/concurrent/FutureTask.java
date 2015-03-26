package ru.ifmo.ctddev.itegulov.concurrent;

import java.util.function.Function;

/**
 * Used for cancellable asynchronous computation. This class provides way
 * to start and cancel a computation, query to see if the computation is
 * complete, and retrieve the result of the computation.  The result can
 * only be retrieved when the computation has completed; the {@link #execute}
 * method will block if the computation has not yet completed.  Once
 * the computation has completed, the computation cannot be cancelled.
 * <p>
 * A {@code FutureTask} can only be used to wrap and execute
 * {@link java.util.function.Function}.
 *
 * @author Daniyar Itegulov
 */
public class FutureTask<R, T> {
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_RUNNING = 1;
    private static final int STATUS_READY = 2;
    private static final int STATUS_CANCELED = 3;

    private final Function<T, R> task;
    private final T argument;
    private volatile R result;
    private volatile int status = STATUS_PENDING;
    private volatile Thread runner = null;

    /**
     * Class constructor, which specify what task to execute and what
     * argument to pass to it. If you will need to interrupt working thread
     * during task's executing, you should provide task, which will check
     * for thread's interrupted state and stop if it is interrupted.
     *
     * @param task task to execute
     * @param argument argument to pass
     */
    public FutureTask(Function<T, R> task, T argument) {
        this.task = task;
        this.argument = argument;
    }

    /**
     * Waits if necessary for the computation to complete, and then
     * saves it's result if not canceled.
     */
    public void execute() {
        synchronized (this) {
            if (status != STATUS_PENDING) {
                return;
            } else {
                status = STATUS_RUNNING;
                runner = Thread.currentThread();
            }
        }
        R result = null;
        try {
            result = task.apply(argument);
        } finally {
            synchronized (this) {
                if (status == STATUS_RUNNING) {
                    this.result = result;
                    status = STATUS_READY;
                }
                runner = null;
            }
        }
    }

    /**
     * Attempts to cancel execution of this task. This attempt will
     * do nothing, if the task has already completed or has already
     * been cancelled. If successful, and this task has not started
     * when {@code cancel} is called, this task should never run.
     * If the task has already started, then it will interrupt working
     * thread.
     * <p>
     * After this method returns, subsequent calls to {@link #isReady} will
     * always return {@code true}.  Subsequent calls to {@link #isCancelled}
     * will always return {@code true} if this method returned {@code true}.
     */
    public void cancel() {
        synchronized (this) {
            if (status != STATUS_READY) {
                status = STATUS_CANCELED;
            }
            if (runner != null) {
                runner.interrupt();
            }
        }
    }

    /**
     * Retrieves result if it's available.
     * <p>
     * Result exists only if {@link #isReady} returns {@code true}.
     * @return result of executing task if it's available, {@code null}
     * otherwise
     */
    public R getResult() {
        synchronized (this) {
            if (status == STATUS_READY) {
                return result;
            }
            return null;
        }
    }

    /**
     * @return {@code true} if task has been completed successfully and
     * it's result is available, {@code false} otherwise.
     */
    public boolean isReady() {
        return status == STATUS_READY;
    }

    /**
     * @return {@code true} if task has been canceled before it completed
     * successfully, {@code false} otherwise.
     */
    public boolean isCancelled() {
        return status == STATUS_CANCELED;
    }

    /**
     * Wait for this future task to be done (ready or cancelled).
     *
     * @throws InterruptedException if this thread was interrupted
     */
    public void waitForDone() throws InterruptedException {
        synchronized (this) {
            while (!isReady() && !isCancelled()) {
                wait();
            }
        }
    }
}
