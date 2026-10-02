package org.sil.hearthis;

import android.util.Log;
import java.io.IOException;
import java.util.Map;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;

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

        Response response;
        if (uri.startsWith("/getfile")) {
            response = requestFileHandler.handle(session);
        } else if (uri.startsWith("/putfile")) {
            response = acceptFileHandler.handle(session);
        } else if (uri.startsWith("/list")) {
            response = listDirectoryHandler.handle(session);
        } else if (uri.startsWith("/notify")) {
            response = acceptNotificationHandler.handle(session);
        } else {
            response = deviceNameHandler.handle(session);
        }
        if (shouldCloseConnection(session.getHeaders(), uri, response)) {
            response.addHeader("Connection", "close");
        }
        return response;
    }

    /**
     * Decides whether to close the connection after this response.
     * A request that carries a body which the handler did not read to the end would leave that
     * body in the socket, where NanoHTTPD would parse it as the start of the next request on a
     * kept-alive connection. In that case we must close. .NET's WebClient reuses connections by
     * default and throws "A connection that was expected to be kept alive was closed by the
     * server" if we close when it didn't expect it, so we keep the connection alive whenever it is
     * safe: for requests without a body, and for a /putfile that was received completely.
     */
    static boolean shouldCloseConnection(Map<String, String> requestHeaders, String uri, Response response) {
        if (!requestHasBody(requestHeaders)) {
            return false;
        }
        if (requestHeaders.containsKey("transfer-encoding")) {
            return true; // chunked bodies are never read by our handlers
        }
        boolean bodyFullyRead = uri.startsWith("/putfile") && response.getStatus() == Response.Status.OK;
        return !bodyFullyRead;
    }

    private static boolean requestHasBody(Map<String, String> headers) {
        if (headers == null) {
            return false;
        }
        if (headers.containsKey("transfer-encoding")) {
            return true;
        }
        String length = headers.get("content-length");
        if (length == null) {
            return false;
        }
        try {
            return Long.parseLong(length.trim()) > 0;
        } catch (NumberFormatException e) {
            return true; // can't tell, so be safe
        }
    }
}
