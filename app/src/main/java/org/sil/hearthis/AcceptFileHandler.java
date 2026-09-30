package org.sil.hearthis;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;

public class AcceptFileHandler {
    private static final String TAG = "AcceptFileHandler";
    private static final String PREFERRED_PART_NAME = "content";
    private static final Pattern BOUNDARY_PATTERN = Pattern.compile("(?i)boundary=\"?([^\";]+)\"?");
    private static final Pattern CONTENT_DISPOSITION_NAME_PATTERN = Pattern.compile("(?i)\\bname=\"([^\"]*)\"");
    private static final Pattern CONTENT_DISPOSITION_FILENAME_PATTERN = Pattern.compile("(?i)\\bfilename=\"([^\"]*)\"");
    private static final byte[] HEADER_TERMINATOR = "\r\n\r\n".getBytes(StandardCharsets.US_ASCII);

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

        // Read the whole request body as raw bytes up front. We never decode any of it through
        // a String: doing so (as NanoHTTPD's own session.parseBody(Map) does for any multipart
        // part that lacks its own Content-Type header) is lossy for binary content - bytes that
        // aren't valid text in whatever charset is used get replaced or dropped, and no
        // re-encoding afterward can recover them.
        byte[] body;
        try {
            // Do NOT close this stream: it is the socket's input stream, and closing it closes
            // the connection before NanoHTTPD can send the response.
            InputStream in = session.getInputStream();
            body = readFully(in, contentLength);
        } catch (IOException e) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "failure: " + e.getMessage() + "\n");
        }

        byte[] data;
        String contentType = session.getHeaders().get("content-type");
        if (contentType != null && contentType.toLowerCase(Locale.US).startsWith("multipart/form-data")) {
            Matcher boundaryMatcher = BOUNDARY_PATTERN.matcher(contentType);
            if (!boundaryMatcher.find()) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT, "Missing multipart boundary\n");
            }
            data = extractMultipartFileContent(body, boundaryMatcher.group(1));
            if (data == null) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT, "failure: no content\n");
            }
        } else {
            data = body;
        }

        if (listener != null) {
            listener.receivingFile(filePath);
        }

        File dir = file.getParentFile();
        if (dir != null && !dir.exists() && !dir.mkdirs()) {
            Log.e(TAG, "Failed to create directory: " + dir.getAbsolutePath());
        }

        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(data);
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

    /**
     * Finds the file part in a multipart/form-data body and returns its raw bytes, operating
     * entirely on byte offsets so the binary content is never routed through a String. Prefers
     * a part named "content"; if none is found by that name, falls back to the first part that
     * declares a filename (i.e. looks like a file upload), since the exact field name used by
     * the desktop client isn't otherwise documented here.
     */
    private static byte[] extractMultipartFileContent(byte[] body, String boundary) {
        byte[] boundaryMarker = ("--" + boundary).getBytes(StandardCharsets.US_ASCII);
        List<Integer> positions = findAll(body, boundaryMarker);

        byte[] preferredMatch = null;
        byte[] firstFileMatch = null;

        for (int i = 0; i < positions.size() - 1; i++) {
            int partStart = positions.get(i) + boundaryMarker.length;
            int partEnd = positions.get(i + 1);

            // Skip the CRLF right after the boundary marker.
            if (partStart + 1 < partEnd && body[partStart] == '\r' && body[partStart + 1] == '\n') {
                partStart += 2;
            }

            int headerEnd = indexOf(body, HEADER_TERMINATOR, partStart, partEnd);
            if (headerEnd < 0) {
                continue;
            }
            String header = new String(body, partStart, headerEnd - partStart, StandardCharsets.US_ASCII);

            Matcher nameMatcher = CONTENT_DISPOSITION_NAME_PATTERN.matcher(header);
            String partName = nameMatcher.find() ? nameMatcher.group(1) : null;
            boolean hasFilename = CONTENT_DISPOSITION_FILENAME_PATTERN.matcher(header).find();

            int dataStart = headerEnd + HEADER_TERMINATOR.length;
            int dataEnd = partEnd;
            // Strip the trailing CRLF that precedes the next boundary.
            if (dataEnd - 2 >= dataStart && body[dataEnd - 2] == '\r' && body[dataEnd - 1] == '\n') {
                dataEnd -= 2;
            }
            if (dataEnd < dataStart) {
                continue;
            }

            byte[] partData = new byte[dataEnd - dataStart];
            System.arraycopy(body, dataStart, partData, 0, partData.length);

            if (PREFERRED_PART_NAME.equals(partName)) {
                preferredMatch = partData;
            } else if (hasFilename && firstFileMatch == null) {
                firstFileMatch = partData;
            }
        }

        return preferredMatch != null ? preferredMatch : firstFileMatch;
    }

    private static List<Integer> findAll(byte[] haystack, byte[] needle) {
        List<Integer> result = new ArrayList<>();
        int from = 0;
        int idx;
        while ((idx = indexOf(haystack, needle, from, haystack.length)) >= 0) {
            result.add(idx);
            from = idx + needle.length;
        }
        return result;
    }

    private static int indexOf(byte[] haystack, byte[] needle, int from, int to) {
        int limit = to - needle.length;
        outer:
        for (int i = Math.max(from, 0); i <= limit; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    public interface IFileReceivedNotification {
        void receivingFile(String name);
    }

    public void setListener(IFileReceivedNotification newListener) {
        listener = newListener;
    }
}
