package org.sil.hearthis;

import android.util.Log;
import java.io.IOException;
import fi.iki.elonen.NanoHTTPD;

/**
 * SyncServer manages the 'web server' for the synchronization service that supports data
 * exchange with HearThis desktop.
 * This is now using NanoHTTPD as the underlying server.
 */
public class SyncServer extends NanoHTTPD {
    private static final String TAG = "SyncServer";
    final SyncService _parent;
    static final int SERVER_PORT = 8087;

    private final DeviceNameHandler deviceNameHandler;
    private final RequestFileHandler requestFileHandler;
    private final AcceptFileHandler acceptFileHandler;
    private final ListDirectoryHandler listDirectoryHandler;
    private final AcceptNotificationHandler acceptNotificationHandler;

    public SyncServer(SyncService parent) {
        super(SERVER_PORT);
        _parent = parent;

        deviceNameHandler = new DeviceNameHandler(_parent);
        requestFileHandler = new RequestFileHandler(_parent);
        acceptFileHandler = new AcceptFileHandler(_parent);
        listDirectoryHandler = new ListDirectoryHandler(_parent);
        acceptNotificationHandler = new AcceptNotificationHandler();
    }

    public RequestFileHandler getRequestFileHandler() {
        return requestFileHandler;
    }

    public AcceptFileHandler getAcceptFileHandler() {
        return acceptFileHandler;
    }

    public AcceptNotificationHandler getAcceptNotificationHandler() {
        return acceptNotificationHandler;
    }

    public synchronized void startThread() {
        if (wasStarted() && isAlive()) {
            Log.d(TAG, "Server already running.");
            return;
        }
        try {
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            Log.d(TAG, "Server started on port " + SERVER_PORT);
        } catch (IOException e) {
            Log.e(TAG, "Could not start server", e);
        }
    }

    public synchronized void stopThread() {
        if (wasStarted()) {
            stop();
            Log.d(TAG, "Server stopped");
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Log.d(TAG, "Serving URI: " + uri);

        if (uri.startsWith("/getfile")) {
            return requestFileHandler.handle(session);
        } else if (uri.startsWith("/putfile")) {
            return acceptFileHandler.handle(session);
        } else if (uri.startsWith("/list")) {
            return listDirectoryHandler.handle(session);
        } else if (uri.startsWith("/notify")) {
            return acceptNotificationHandler.handle(session);
        } else {
            return deviceNameHandler.handle(session);
        }
    }
}
