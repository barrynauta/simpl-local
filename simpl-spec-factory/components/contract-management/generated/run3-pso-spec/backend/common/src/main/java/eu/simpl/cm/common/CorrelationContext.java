package eu.simpl.cm.common;

import java.util.UUID;
import java.util.concurrent.Callable;

/** Holds the correlation id of the current request or background task. */
public final class CorrelationContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private CorrelationContext() {
    }

    public static void set(String id) {
        CURRENT.set(id);
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static String get() {
        return CURRENT.get();
    }

    public static String getOrCreate() {
        String id = CURRENT.get();
        if (id == null) {
            id = UUID.randomUUID().toString();
            CURRENT.set(id);
        }
        return id;
    }

    /** Wraps a task so that it runs with the given correlation id. */
    public static Runnable wrap(String id, Runnable task) {
        return () -> {
            String previous = CURRENT.get();
            CURRENT.set(id);
            try {
                task.run();
            } finally {
                if (previous == null) {
                    CURRENT.remove();
                } else {
                    CURRENT.set(previous);
                }
            }
        };
    }

    public static <T> Callable<T> wrap(String id, Callable<T> task) {
        return () -> {
            CURRENT.set(id);
            try {
                return task.call();
            } finally {
                CURRENT.remove();
            }
        };
    }
}
