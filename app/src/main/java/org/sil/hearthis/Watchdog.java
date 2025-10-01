package org.sil.hearthis;

import android.util.Log;

//import org.apache.http.HttpException;
//import org.apache.http.impl.DefaultConnectionReuseStrategy;
//import org.apache.http.impl.DefaultHttpResponseFactory;
//import org.apache.http.impl.DefaultHttpServerConnection;
//import org.apache.http.params.BasicHttpParams;
//import org.apache.http.protocol.BasicHttpContext;
//import org.apache.http.protocol.BasicHttpProcessor;
//import org.apache.http.protocol.HttpRequestHandlerRegistry;
//import org.apache.http.protocol.HttpService;
//import org.apache.http.protocol.ResponseConnControl;
//import org.apache.http.protocol.ResponseContent;
//import org.apache.http.protocol.ResponseDate;
//import org.apache.http.protocol.ResponseServer;

//import java.io.IOException;
//import java.net.ServerSocket;
//import java.net.Socket;
import java.util.concurrent.*;

/**
 * This class implements a timeout for the Android side of a HearThis sync operation.
 *
 */

public class Watchdog {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> watchdogTask;
    private final Runnable onTimeout;
    private final long timeout;
    private final TimeUnit unit;

    public Watchdog(long timeout, TimeUnit unit, Runnable onTimeout) {
        Log.d("Sync", "Watchdog, constructor begin, timeout   = " + timeout); // WM, temporary
        Log.d("Sync", "                             unit      = " + unit); // WM, temporary
        this.timeout = timeout;
        this.unit = unit;
        this.onTimeout = onTimeout;
    }

    // Call this whenever input is received
    public synchronized void pet() {
        if (watchdogTask != null && !watchdogTask.isDone()) {
            Log.d("Sync", "Watchdog, pet, setting cancel false"); // WM, temporary
            watchdogTask.cancel(false);
        }
        Log.d("Sync", "Watchdog, pet, calling scheduler.schedule()"); // WM, temporary
        watchdogTask = scheduler.schedule(onTimeout, timeout, unit);
    }

    public void shutdown() {
        Log.d("Sync", "Watchdog, shutting down"); // WM, temporary
        scheduler.shutdownNow();
    }
}
