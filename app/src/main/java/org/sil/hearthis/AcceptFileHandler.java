package org.sil.hearthis;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;

public class AcceptFileHandler {
    private static final String TAG = "AcceptFileHandler";
    // Upper bound on a single uploaded file, to stop a bogus Content-Length from filling storage.
    static final long MAX_UPLOAD_BYTES = 100L * 1024 * 1024;

    // NanoHTTPD 2.3.1 has no built-in 413 status.
    private static final Response.IStatus PAYLOAD_TOO_LARGE = new Response.IStatus() {
        @Override
        public int getRequestStatus() {
            return 413;
        }

        @Override
        public String getDescription() {
            return "413 Payload Too Large";
        }
    };

    private final Context _parent;
    private IFileReceivedNotification listener;

    public AcceptFileHandler(Context parent) {
        _parent = parent;
    }

    public Response handle(NanoHTTPD.IHTTPSession session) {
        File baseDir = _parent.getExternalFilesDir(null);
        if (baseDir == null) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "External storage not available");
        }

        List<String> pathParams = session.getParameters().get("path");
        String filePath = (pathParams != null && !pathParams.isEmpty())
                ? pathParams.get(0).replace('\\', '/')
                : null;

        if (filePath == null) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT, "Missing path parameter");
        }

        // Fix Path Traversal Vulnerability
        File file = new File(baseDir, filePath);
        try {
            // Verify path is inside baseDir to prevent traversal attacks
            String canonicalBase = baseDir.getCanonicalPath();
            String canonicalRequested = file.getCanonicalPath();
            // Require a separator after the base so a sibling like "<base>-evil" doesn't match.
            if (!canonicalRequested.startsWith(canonicalBase + File.separator)) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "Access denied");
            }
        } catch (IOException e) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "Error validating path");
        }

        String contentLengthHeader = session.getHeaders().get("content-length");
        long contentLength;
        try {
            contentLength = contentLengthHeader != null ? Long.parseLong(contentLengthHeader.trim()) : -1;
        } catch (NumberFormatException e) {
            contentLength = -1;
        }
        if (contentLength < 0) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT, "Missing or invalid Content-Length");
        }
        if (contentLength > MAX_UPLOAD_BYTES) {
            return NanoHTTPD.newFixedLengthResponse(PAYLOAD_TOO_LARGE, NanoHTTPD.MIME_PLAINTEXT, "File too large");
        }

        if (listener != null) {
            listener.receivingFile(filePath);
        }

        File dir = file.getParentFile();
        if (dir != null && !dir.exists() && !dir.mkdirs()) {
            Log.e(TAG, "Failed to create directory: " + dir.getAbsolutePath());
        }

        // Stream the raw bytes (not a String, which would corrupt binary content) into a temp
        // file, and only move it into place once the whole body has arrived. That way a dropped
        // connection never leaves a truncated file, and the body is never held in memory.
        File temp = new File(dir, file.getName() + ".part");
        try {
            // Do NOT close this stream: it is the socket's input stream, and closing it closes
            // the connection before NanoHTTPD can send the response. That results in the PC side
            // suffering an unhandled System.Net.WebException.
            InputStream in = session.getInputStream();
            try (FileOutputStream out = new FileOutputStream(temp)) {
                copyExactly(in, out, contentLength);
            }
            if (file.exists() && !file.delete()) {
                throw new IOException("Could not replace existing file");
            }
            if (!temp.renameTo(file)) {
                throw new IOException("Could not move received file into place");
            }
        } catch (IOException e) {
            if (temp.exists() && !temp.delete()) {
                Log.e(TAG, "Failed to delete partial file: " + temp.getAbsolutePath());
            }
            return NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "failure: " + e.getMessage());
        }
        return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "success");
    }

    private static void copyExactly(InputStream in, OutputStream out, long length) throws IOException {
        byte[] buf = new byte[8192];
        long remaining = length;
        while (remaining > 0) {
            int read = in.read(buf, 0, (int) Math.min(buf.length, remaining));
            if (read < 0) {
                throw new IOException("Unexpected end of stream reading request body");
            }
            out.write(buf, 0, read);
            remaining -= read;
        }
    }

    public interface IFileReceivedNotification {
        void receivingFile(String name);
    }

    public void setListener(IFileReceivedNotification newListener) {
        listener = newListener;
    }
}
