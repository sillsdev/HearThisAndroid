package org.sil.hearthis;

import java.util.concurrent.*;
import android.util.Log;

/**
 * This class implements a "watchdog" timer for the Android side of a HearThis sync operation.
 *
 * Once instantiated and started, it counts down from its timeout value (passed in). The timer
 * is NOT supposed to get all the way down to 0. If it does, a problematic condition has arisen
 * somewhere and the 'onTimeout' code runs in an effort to mitigate the problem.
 * Calling pet() restarts a full countdown. The timeout value should be chosen such that it is
 * longer than any normal interval between calls to pet(). Thus in a correctly working system,
 * pet() keeps getting called well before the timer ever finishes counting down to 0 from its
 * initial timeout value, and the 'onTimeout' code never runs.
 */

public class Watchdog {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> watchdogTask;
    private final Runnable onTimeout;
    private final long timeout;
    private final TimeUnit unit;

    public Watchdog(long timeout, TimeUnit unit, Runnable onTimeout) {
        this.timeout = timeout;
        this.unit = unit;
        this.onTimeout = onTimeout;
    }

    // Subsystems of interest call this method to restart the timer countdown. Basically this
    // means: "At the moment all is well. We'll try to call again before your next deadline. If
    // we don't, send for help."
    public synchronized void pet() {
        if (watchdogTask != null && !watchdogTask.isDone()) {
            watchdogTask.cancel(false);
        }
        watchdogTask = scheduler.schedule(onTimeout, timeout, unit);
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}
