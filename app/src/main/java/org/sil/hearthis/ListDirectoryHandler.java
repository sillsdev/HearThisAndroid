package org.sil.hearthis;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;

/**
 * Created by Thomson on 12/28/2014.
 */
public class ListDirectoryHandler {
    private static final String TAG = "ListDirectoryHandler";
    private final Context _parent;

    public ListDirectoryHandler(Context parent) {
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
                : "";

        // Fix Path Traversal Vulnerability
        File file = new File(baseDir, filePath);
        try {
            if (!file.getCanonicalPath().startsWith(baseDir.getCanonicalPath())) {
                Log.w(TAG, "Attempted path traversal: " + filePath);
                return NanoHTTPD.newFixedLengthResponse(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "Access denied");
            }
        } catch (IOException e) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "Error validating path");
        }

        StringBuilder sb = new StringBuilder();
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                df.setTimeZone(TimeZone.getTimeZone("UTC"));
                for (File f : files) {
                    sb.append(f.getName());
                    sb.append(";");
                    sb.append(df.format(new Date(f.lastModified())));
                    sb.append(";");
                    sb.append(f.isDirectory() ? "d" : "f");
                    sb.append("\n");
                }
            }
            return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, sb.toString());
        } else {
            // If it's not a directory, just return empty list or NOT_FOUND? 
            // Original code returned empty string for non-directories.
            return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "");
        }
    }
}
