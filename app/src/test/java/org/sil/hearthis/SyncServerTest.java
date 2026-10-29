package org.sil.hearthis;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;

/**
 * Tests the keep-alive decision in SyncServer: close the connection only when a request body
 * may have been left unread.
 */
public class SyncServerTest {

    private static Response response(Response.Status status) {
        return NanoHTTPD.newFixedLengthResponse(status, NanoHTTPD.MIME_PLAINTEXT, "");
    }

    private static Map<String, String> headers(String... keyValues) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    @Test
    public void requestWithoutBody_keepsConnectionAlive() {
        assertFalse(SyncServer.shouldCloseConnection(headers(), "/list", response(Response.Status.OK)));
        assertFalse(SyncServer.shouldCloseConnection(headers(), "/getfile", response(Response.Status.NOT_FOUND)));
        assertFalse(SyncServer.shouldCloseConnection(headers(), "/", response(Response.Status.OK)));
    }

    @Test
    public void zeroContentLength_keepsConnectionAlive() {
        assertFalse(SyncServer.shouldCloseConnection(headers("content-length", "0"), "/notify", response(Response.Status.OK)));
    }

    @Test
    public void nullHeaders_keepsConnectionAlive() {
        assertFalse(SyncServer.shouldCloseConnection(null, "/list", response(Response.Status.OK)));
    }

    @Test
    public void successfulPutFile_keepsConnectionAlive() {
        assertFalse(SyncServer.shouldCloseConnection(headers("content-length", "1000"), "/putfile?path=a.txt", response(Response.Status.OK)));
    }

    @Test
    public void failedPutFile_closesConnection() {
        Map<String, String> h = headers("content-length", "1000");
        assertTrue(SyncServer.shouldCloseConnection(h, "/putfile?path=a.txt", response(Response.Status.FORBIDDEN)));
        assertTrue(SyncServer.shouldCloseConnection(h, "/putfile?path=a.txt", response(Response.Status.INTERNAL_ERROR)));
        assertTrue(SyncServer.shouldCloseConnection(h, "/putfile?path=a.txt", response(Response.Status.BAD_REQUEST)));
    }

    @Test
    public void notifyWithBody_closesConnectionBecauseBodyIsNotRead() {
        assertTrue(SyncServer.shouldCloseConnection(headers("content-length", "5"), "/notify", response(Response.Status.OK)));
    }

    @Test
    public void chunkedRequest_closesConnection() {
        assertTrue(SyncServer.shouldCloseConnection(headers("transfer-encoding", "chunked"), "/putfile", response(Response.Status.OK)));
    }

    @Test
    public void unparseableContentLength_closesConnection() {
        assertTrue(SyncServer.shouldCloseConnection(headers("content-length", "abc"), "/list", response(Response.Status.OK)));
    }
}
