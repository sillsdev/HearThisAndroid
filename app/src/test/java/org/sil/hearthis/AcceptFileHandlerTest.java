package org.sil.hearthis;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.ContextWrapper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;

/**
 * Unit tests for AcceptFileHandler.
 * Uses Dynamic Proxies and ContextWrappers to provide a clean testing environment
 * without Mockito warnings or deprecated method implementations.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class AcceptFileHandlerTest {

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    private AcceptFileHandler handler;
    private File baseDir;
    private TestFileReceivedNotification mockListener;

    @Before
    public void setUp() throws IOException {
        baseDir = tempFolder.newFolder("externalFiles");

        // Wrap the Robolectric context to override only the directory logic.
        Context context = new ContextWrapper(RuntimeEnvironment.getApplication()) {
            @Override
            public File getExternalFilesDir(String type) {
                return baseDir;
            }
        };

        handler = new AcceptFileHandler(context);
        mockListener = new TestFileReceivedNotification();
        handler.setListener(mockListener);
    }

    @Test
    public void handle_savesRawContentToFile() throws Exception {
        byte[] body = "Genesis;10:0".getBytes(StandardCharsets.UTF_8);

        try (Response response = handler.handle(createSession("ProjectA/info.txt", body))) {
            assertEquals(Response.Status.OK, response.getStatus());
            File savedFile = new File(baseDir, "ProjectA/info.txt");
            assertTrue("File should be saved", savedFile.exists());
            assertArrayEquals(body, Files.readAllBytes(savedFile.toPath()));
        }
    }

    @Test
    public void handle_savesBinaryContentUnchanged() throws Exception {
        // Includes bytes that are not valid UTF-8, which would be corrupted by a String round trip.
        byte[] body = new byte[1000];
        for (int i = 0; i < body.length; i++) {
            body[i] = (byte) (i * 7);
        }
        body[0] = (byte) 0xFF;
        body[1] = (byte) 0xFE;

        try (Response response = handler.handle(createSession("ProjectA/audio/1.wav", body))) {
            assertEquals(Response.Status.OK, response.getStatus());
            assertArrayEquals(body, Files.readAllBytes(new File(baseDir, "ProjectA/audio/1.wav").toPath()));
        }
    }

    @Test
    public void handle_savesBodyLargerThanOneChunk() throws Exception {
        byte[] body = new byte[8192 * 3 + 123];
        for (int i = 0; i < body.length; i++) {
            body[i] = (byte) (i % 251);
        }

        try (Response response = handler.handle(createSession("big.bin", body))) {
            assertEquals(Response.Status.OK, response.getStatus());
            assertArrayEquals(body, Files.readAllBytes(new File(baseDir, "big.bin").toPath()));
        }
    }

    @Test
    public void handle_savesEmptyBody() throws Exception {
        try (Response response = handler.handle(createSession("empty.txt", new byte[0]))) {
            assertEquals(Response.Status.OK, response.getStatus());
            File savedFile = new File(baseDir, "empty.txt");
            assertTrue(savedFile.exists());
            assertEquals(0, savedFile.length());
        }
    }

    @Test
    public void handle_overwritesExistingFile() throws Exception {
        File target = new File(baseDir, "info.txt");
        Files.write(target.toPath(), "old content that is longer".getBytes(StandardCharsets.UTF_8));
        byte[] body = "new".getBytes(StandardCharsets.UTF_8);

        try (Response response = handler.handle(createSession("info.txt", body))) {
            assertEquals(Response.Status.OK, response.getStatus());
            assertArrayEquals(body, Files.readAllBytes(target.toPath()));
        }
    }

    @Test
    public void handle_leavesNoTempFileAfterSuccess() throws Exception {
        try (Response ignored = handler.handle(createSession("ProjectA/info.txt", new byte[]{1, 2, 3}))) {
            assertNoPartFiles();
        }
    }

    @Test
    public void handle_notifiesListener() throws Exception {
        try (Response ignored = handler.handle(createSession("test.wav", new byte[]{1}))) {
            assertEquals("test.wav", mockListener.receivedFileName);
        }
    }

    @Test
    public void handle_success_doesNotReportFailure() throws Exception {
        try (Response ignored = handler.handle(createSession("ok.txt", new byte[]{1}))) {
            assertNull(mockListener.failedFileName);
        }
    }

    @Test
    public void handle_missingPathParameter_returnsBadRequest() throws Exception {
        try (Response response = handler.handle(createSession(null, new byte[]{1}))) {
            assertEquals(Response.Status.BAD_REQUEST, response.getStatus());
        }
    }

    // ---- Path traversal ----

    @Test
    public void handle_preventsPathTraversal() throws IOException {
        try (Response response = handler.handle(createSession("../secret.txt", "data".getBytes(StandardCharsets.UTF_8)))) {
            assertEquals(Response.Status.FORBIDDEN, response.getStatus());
            File secretFile = new File(baseDir.getParentFile(), "secret.txt");
            assertFalse("File should NOT be saved outside baseDir", secretFile.exists());
        }
    }

    @Test
    public void handle_rejectsSiblingDirectoryWithSamePrefix() throws IOException {
        // "externalFiles-evil" starts with the canonical base path "externalFiles" but is not inside it.
        String path = "../" + baseDir.getName() + "-evil/secret.txt";

        try (Response response = handler.handle(createSession(path, "data".getBytes(StandardCharsets.UTF_8)))) {
            assertEquals(Response.Status.FORBIDDEN, response.getStatus());
            File sibling = new File(baseDir.getParentFile(), baseDir.getName() + "-evil");
            assertFalse("Sibling directory should NOT be created", sibling.exists());
            assertNull("Listener should not be notified", mockListener.receivedFileName);
        }
    }

    @Test
    public void handle_rejectsPathEqualToBaseDirectory() throws IOException {
        try (Response response = handler.handle(createSession(".", new byte[]{1}))) {
            assertEquals(Response.Status.FORBIDDEN, response.getStatus());
        }
    }

    @Test
    public void handle_rejectsBackslashTraversal() throws IOException {
        // Backslashes are normalized to '/', so this must still be caught.
        try (Response response = handler.handle(createSession("..\\secret.txt", new byte[]{1}))) {
            assertEquals(Response.Status.FORBIDDEN, response.getStatus());
            assertFalse(new File(baseDir.getParentFile(), "secret.txt").exists());
        }
    }

    // ---- Content-Length handling ----

    @Test
    public void handle_missingContentLength_returnsLengthRequired() throws Exception {
        try (Response response = handler.handle(createSession("a.txt", new byte[]{1}, null))) {
            assertEquals(411, response.getStatus().getRequestStatus());
            assertFalse(new File(baseDir, "a.txt").exists());
        }
    }

    @Test
    public void handle_nonNumericContentLength_returnsLengthRequired() throws Exception {
        try (Response response = handler.handle(createSession("a.txt", new byte[]{1}, "abc"))) {
            assertEquals(411, response.getStatus().getRequestStatus());
            assertFalse(new File(baseDir, "a.txt").exists());
        }
    }

    @Test
    public void handle_negativeContentLength_returnsLengthRequired() throws Exception {
        try (Response response = handler.handle(createSession("a.txt", new byte[]{1}, "-5"))) {
            assertEquals(411, response.getStatus().getRequestStatus());
            assertFalse(new File(baseDir, "a.txt").exists());
        }
    }

    @Test
    public void handle_contentLengthOverLimit_returns413WithoutReadingBody() throws Exception {
        String tooBig = String.valueOf(AcceptFileHandler.MAX_UPLOAD_BYTES + 1);
        CountingInputStream body = new CountingInputStream(new byte[]{1, 2, 3});

        try (Response response = handler.handle(createSession("big.wav", body, tooBig))) {
            assertEquals(413, response.getStatus().getRequestStatus());
            assertEquals("Body must not be read for an oversized upload", 0, body.bytesRead);
            assertFalse(new File(baseDir, "big.wav").exists());
            assertNull("Listener should not be notified", mockListener.receivedFileName);
            assertNoPartFiles();
        }
    }

    @Test
    public void handle_hugeContentLengthThatOverflowsInt_returns413() throws Exception {
        // Larger than Integer.MAX_VALUE: must not be mis-parsed as invalid or overflow.
        try (Response response = handler.handle(createSession("big.wav", new byte[]{1}, "99999999999"))) {
            assertEquals(413, response.getStatus().getRequestStatus());
            assertFalse(new File(baseDir, "big.wav").exists());
        }
    }

    @Test
    public void handle_contentLengthExactlyAtLimit_isNotRejectedForSize() throws Exception {
        // Don't actually send 100 MB: the stream ends early, so we expect a truncation error
        // (500), but crucially not the 413 or 400 that a size/format check would produce.
        String atLimit = String.valueOf(AcceptFileHandler.MAX_UPLOAD_BYTES);

        try (Response response = handler.handle(createSession("limit.wav", new byte[]{1}, atLimit))) {
            assertEquals(Response.Status.INTERNAL_ERROR, response.getStatus());
        }
    }

    // ---- Truncated / failed uploads ----

    @Test
    public void handle_truncatedBody_returnsErrorAndLeavesNoFile() throws Exception {
        byte[] actual = new byte[100];

        // Claims 500 bytes but the connection delivers only 100.
        try (Response response = handler.handle(createSession("ProjectA/1.wav", actual, "500"))) {
            assertEquals(Response.Status.INTERNAL_ERROR, response.getStatus());
            assertFalse("Truncated file must not be left in place", new File(baseDir, "ProjectA/1.wav").exists());
            assertNoPartFiles();
            assertEquals("ProjectA/1.wav", mockListener.receivedFileName);
            assertEquals("Listener should be told the receive failed", "ProjectA/1.wav", mockListener.failedFileName);
        }
    }

    @Test
    public void handle_truncatedBody_doesNotDestroyExistingFile() throws Exception {
        File target = new File(baseDir, "1.wav");
        byte[] original = "original recording".getBytes(StandardCharsets.UTF_8);
        Files.write(target.toPath(), original);

        try (Response response = handler.handle(createSession("1.wav", new byte[10], "500"))) {
            assertEquals(Response.Status.INTERNAL_ERROR, response.getStatus());
            assertArrayEquals("Existing file must survive a failed upload", original, Files.readAllBytes(target.toPath()));
            assertNoPartFiles();
        }
    }

    @Test
    public void handle_readsOnlyContentLengthBytes() throws Exception {
        // The socket may carry more data than Content-Length (e.g. a following request).
        byte[] stream = "0123456789EXTRA".getBytes(StandardCharsets.UTF_8);

        try (Response response = handler.handle(createSession("a.txt", stream, "10"))) {
            assertEquals(Response.Status.OK, response.getStatus());
            assertEquals("0123456789", new String(Files.readAllBytes(new File(baseDir, "a.txt").toPath()), StandardCharsets.UTF_8));
        }
    }

    // ---- Helpers ----

    private void assertNoPartFiles() throws IOException {
        try (var paths = Files.walk(baseDir.toPath())) {
            List<String> leftovers = paths
                    .map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".part"))
                    .toList();
            assertTrue("Leftover temp files: " + leftovers, leftovers.isEmpty());
        }
    }

    private IHTTPSession createSession(String path, byte[] body) {
        return createSession(path, body, String.valueOf(body.length));
    }

    private IHTTPSession createSession(String path, byte[] body, String contentLength) {
        return createSession(path, new CountingInputStream(body), contentLength);
    }

    private IHTTPSession createSession(String path, CountingInputStream body, String contentLength) {
        Map<String, String> headers = new HashMap<>();
        if (contentLength != null) {
            headers.put("content-length", contentLength);
        }
        Map<String, List<String>> params = new HashMap<>();
        if (path != null) {
            params.put("path", List.of(path));
        }
        return createMockSession(params, headers, body);
    }

    /**
     * Creates a dynamic proxy for IHTTPSession. This allows us to implement
     * the modern getParameters() method without writing source code for
     * the deprecated getParms() method.
     */
    private IHTTPSession createMockSession(Map<String, List<String>> params, Map<String, String> headers, CountingInputStream body) {
        return (IHTTPSession) Proxy.newProxyInstance(
                IHTTPSession.class.getClassLoader(),
                new Class<?>[]{IHTTPSession.class},
                (proxy, method, args) -> {
                    String methodName = method.getName();
                    switch (methodName) {
                        case "getParameters" -> {
                            return params;
                        }
                        case "getHeaders" -> {
                            return headers;
                        }
                        case "getInputStream" -> {
                            return body;
                        }
                        case "toString" -> {
                            return "MockSession";
                        }
                        case "hashCode" -> {
                            return System.identityHashCode(proxy);
                        }
                        case "equals" -> {
                            return proxy == (args != null ? args[0] : null);
                        }
                        default -> {
                            // By returning null here, we avoid referencing getParms() in source code
                            return null;
                        }
                    }
                }
        );
    }

    /** An in-memory request body that records how many bytes the handler consumed. */
    private static class CountingInputStream extends ByteArrayInputStream {
        int bytesRead;

        CountingInputStream(byte[] data) {
            super(data);
        }

        @Override
        public synchronized int read(byte[] b, int off, int len) {
            int n = super.read(b, off, len);
            if (n > 0) {
                bytesRead += n;
            }
            return n;
        }

        @Override
        public synchronized int read() {
            int b = super.read();
            if (b >= 0) {
                bytesRead++;
            }
            return b;
        }
    }

    private static class TestFileReceivedNotification implements AcceptFileHandler.IFileReceivedNotification {
        String receivedFileName;
        String failedFileName;
        @Override
        public void receivingFile(String name) {
            receivedFileName = name;
        }

        @Override
        public void receiveFailed(String name) {
            failedFileName = name;
        }
    }
}
