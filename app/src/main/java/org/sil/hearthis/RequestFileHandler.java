package org.sil.hearthis;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;

/**
 * Created by Thomson on 12/28/2014.
 */
public class RequestFileHandler {
    private static final String TAG = "RequestFileHandler";
    final Context _parent;
    // Set from the UI thread, read from NanoHTTPD worker threads. Read it once into a local
    // before use so a concurrent setListener(null) can't cause an NPE between check and call.
    private volatile IFileSentNotification listener;

    public RequestFileHandler(Context parent) {
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
            return NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST,
                    NanoHTTPD.MIME_PLAINTEXT, "Missing path parameter");
        }

        File file = new File(baseDir, filePath);
        // Path traversal check.
        Response denied = PathGuard.check(baseDir, file, filePath, true);
        if (denied != null) {
            return denied;
        }

        IFileSentNotification l = listener;
        if (l != null) {
            l.sendingFile(filePath);
        }
        
        if (!file.exists() || !file.isFile()) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "File not found");
        }

        try {
            // NanoHTTPD takes care of closing the stream if we pass it to the response.
            FileInputStream fis = new FileInputStream(file);
            Response response = NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "application/octet-stream", fis, file.length());
            response.addHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
            return response;
        } catch (IOException e) {
            Log.e(TAG, "Error reading file: " + file.getAbsolutePath(), e);
            return NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "Error reading file");
        }
    }

    public interface IFileSentNotification {
        void sendingFile(String name);
    }

    public void setListener(IFileSentNotification newListener) {
        listener = newListener;
    }
}
