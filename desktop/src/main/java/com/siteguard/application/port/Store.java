package com.siteguard.application.port;

/** Unit-of-work port. Blocking, so never call it from the Swing event thread. */
public interface Store {
    @FunctionalInterface
    interface Work<T> { T run(Repositories repositories); }

    /** Run atomically: commit on return, roll back on any exception. */
    <T> T inTransaction(Work<T> work);

    /** Run read-only on one connection. */
    <T> T read(Work<T> work);
}
