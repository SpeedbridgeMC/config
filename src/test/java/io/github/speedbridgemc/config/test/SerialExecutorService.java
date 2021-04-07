package io.github.speedbridgemc.config.test;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public final class SerialExecutorService extends AbstractExecutorService {
    private BlockingDeque<Runnable> commands;

    public SerialExecutorService() {
        commands = new LinkedBlockingDeque<>();
    }

    public void process() {
        Runnable command;
        while ((command = commands.poll()) != null)
            command.run();
    }

    @Override
    public void execute(@NotNull Runnable command) {
        if (commands == null)
            throw new RejectedExecutionException("Executor already shutdown");
        if (!commands.offer(command))
            throw new RejectedExecutionException("Failed to add command to deque");
    }

    @Override
    public void shutdown() {
        process();
        commands = null;
    }

    @NotNull
    @Override
    public List<Runnable> shutdownNow() {
        ArrayList<Runnable> list = new ArrayList<>(commands);
        commands = null;
        return list;
    }

    @Override
    public boolean isShutdown() {
        return commands == null;
    }

    @Override
    public boolean isTerminated() {
        return commands == null;
    }

    @Override
    public boolean awaitTermination(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
        long end = System.nanoTime() + unit.toNanos(timeout);
        Runnable command;
        while ((command = commands.poll()) != null) {
            command.run();
            if (Thread.interrupted())
                throw new InterruptedException();
            if (System.nanoTime() >= end) {
                commands = null;
                return false;
            }
        }
        commands = null;
        return true;
    }
}
