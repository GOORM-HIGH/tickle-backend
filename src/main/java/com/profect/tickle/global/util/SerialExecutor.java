package com.profect.tickle.global.util;

import jakarta.validation.constraints.NotNull;

import java.util.ArrayDeque;
import java.util.concurrent.Executor;

public final class SerialExecutor implements Executor {
    private final Executor backend;
    private final ArrayDeque<Runnable> tasks = new ArrayDeque<>();
    private Runnable active;

    public SerialExecutor(Executor backend) {
        this.backend = backend;
    }

    @Override
    public synchronized void execute(@NotNull Runnable r) {
        tasks.add(() -> {
            try {
                r.run();
            } finally {
                scheduleNext();
            }
        });
        if (active == null) scheduleNext();
    }

    private synchronized void scheduleNext() {
        if ((active = tasks.poll()) != null) {
            backend.execute(active);
        }
    }
}
