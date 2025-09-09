package dev.max.quickfix.client.time;

import lombok.experimental.UtilityClass;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@UtilityClass
public final class TimeUtils {

    private static final Executor timeoutExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public static void onTimeout(long timeout, Runnable onTimeout) {
        timeoutExecutor.execute(() -> {
            try {
                Thread.sleep(timeout);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            onTimeout.run();
        });

    }
}
