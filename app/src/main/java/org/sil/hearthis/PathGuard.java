package org.sil.hearthis;

import android.util.Log;

import java.io.File;
import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;

/**
 * The single path-traversal check shared by the file HTTP handlers. Tighten the rule here and
 * every handler picks it up.
 */
final class PathGuard {
    private static final String TAG = "PathGuard";

    private PathGuard() {
    }

    /**
     * Checks that {@code file} (built from the client-supplied {@code requestedPath}) resolves
     * to somewhere inside {@code baseDir}.
     *
     * @param allowBaseDir whether the request may resolve to exactly {@code baseDir} (e.g. listing
     *                     the root). Handlers that write to the path must pass false.
     * @return null if the path is acceptable, otherwise the error response to return to the client.
     */
    static Response check(File baseDir, File file, String requestedPath, boolean allowBaseDir) {
        try {
            String canonicalBase = baseDir.getCanonicalPath();
            String canonicalRequested = file.getCanonicalPath();
            // Require a separator after the base so a sibling like "<base>-evil" doesn't match.
            boolean inside = canonicalRequested.startsWith(canonicalBase + File.separator)
                    || (allowBaseDir && canonicalRequested.equals(canonicalBase));
            if (!inside) {
                Log.w(TAG, "Attempted path traversal: " + requestedPath);
                return NanoHTTPD.newFixedLengthResponse(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "Access denied");
            }
            return null;
        } catch (IOException e) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "Error validating path");
        }
    }
}
