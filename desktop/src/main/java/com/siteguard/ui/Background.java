package com.siteguard.ui;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.SwingWorker;

/** Runs a blocking service call off the Swing thread and delivers the result on it. */
final class Background {
    private Background() { }

    static <T> void run(Callable<T> task, Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception {
                return task.call();
            }

            @Override protected void done() {
                try {
                    T value = get();
                    if (onSuccess != null) onSuccess.accept(value);
                } catch (ExecutionException e) {
                    if (onFailure != null) onFailure.accept(e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }
}
