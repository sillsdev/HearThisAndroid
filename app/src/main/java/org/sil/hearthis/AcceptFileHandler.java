package org.sil.hearthis;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;

public class AcceptFileHandler {
    private static final String TAG = "AcceptFileHandler";

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
            if (!canonicalRequested.startsWith(canonicalBase)) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "Access denied");
            }
        } catch (IOException e) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "Error validating path");
        }

        String contentLengthHeader = session.getHeaders().get("content-length");
        int contentLength;
        try {
            contentLength = contentLengthHeader != null ? Integer.parseInt(contentLengthHeader) : -1;
        } catch (NumberFormatException e) {
            contentLength = -1;
        }
        if (contentLength < 0) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT, "Missing or invalid Content-Length\n");
        }

        // Read the body as raw bytes, not as a String (because then, if the content is binary,
        // bytes that are not valid text would get replaced or dropped).
        byte[] body;
        try {
            // Do NOT close this stream: it is the socket's input stream, and closing it closes
            // the connection before NanoHTTPD can send the response. That results in the PC side
            // suffering an unhandled System.Net.WebException.
            InputStream in = session.getInputStream();
            body = readFully(in, contentLength);
        } catch (IOException e) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "failure: " + e.getMessage() + "\n");
        }

        if (listener != null) {
            listener.receivingFile(filePath);
        }

        File dir = file.getParentFile();
        if (dir != null && !dir.exists() && !dir.mkdirs()) {
            Log.e(TAG, "Failed to create directory: " + dir.getAbsolutePath());
        }

        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(body);
        } catch (IOException e) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "failure: " + e.getMessage() + "\n");
        }
        return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "success\n");
    }

    private static byte[] readFully(InputStream in, int length) throws IOException {
        byte[] result = new byte[length];
        int offset = 0;
        while (offset < length) {
            int read = in.read(result, offset, length - offset);
            if (read < 0) {
                throw new IOException("Unexpected end of stream reading request body");
            }
            offset += read;
        }
        return result;
    }

    public interface IFileReceivedNotification {
        void receivingFile(String name);
    }

    public void setListener(IFileReceivedNotification newListener) {
        listener = newListener;
    }
}
