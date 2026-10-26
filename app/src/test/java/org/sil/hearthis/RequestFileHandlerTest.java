package org.sil.hearthis;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
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
 * Unit tests for RequestFileHandler, focused on path traversal protection.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class RequestFileHandlerTest {

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    private RequestFileHandler handler;
    private File baseDir;
    private final byte[] content = "Genesis;10:0".getBytes(StandardCharsets.UTF_8);

    @Before
    public void setUp() throws IOException {
        baseDir = tempFolder.newFolder("externalFiles");
        // Shares baseDir's name as a prefix, so a bare startsWith check would accept it.
        File siblingDir = tempFolder.newFolder("externalFiles-secret");
        assertTrue(new File(baseDir, "ProjectA").mkdir());
        Files.write(new File(baseDir, "ProjectA/info.txt").toPath(), content);
        Files.write(new File(siblingDir, "secret.txt").toPath(), "s".getBytes(StandardCharsets.UTF_8));
        Files.write(new File(tempFolder.getRoot(), "outside.txt").toPath(), "o".getBytes(StandardCharsets.UTF_8));

        Context context = new ContextWrapper(RuntimeEnvironment.getApplication()) {
            @Override
            public File getExternalFilesDir(String type) {
                return baseDir;
            }
        };
        handler = new RequestFileHandler(context);
    }

    @Test
    public void handle_returnsFileContent() throws Exception {
        try (Response response = handler.handle(createSession("ProjectA/info.txt"))) {
            assertEquals(Response.Status.OK, response.getStatus());
            assertArrayEquals(content, response.getData().readAllBytes());
        }
    }

    @Test
    public void handle_missingFileIsNotFound() throws Exception {
        try (Response response = handler.handle(createSession("ProjectA/nope.txt"))) {
            assertEquals(Response.Status.NOT_FOUND, response.getStatus());
        }
    }

    @Test
    public void handle_missingPathIsBadRequest() throws Exception {
        try (Response response = handler.handle(createSession(null))) {
            assertEquals(Response.Status.BAD_REQUEST, response.getStatus());
        }
    }

    @Test
    public void handle_parentTraversalIsForbidden() throws Exception {
        try (Response response = handler.handle(createSession("../outside.txt"))) {
            assertEquals(Response.Status.FORBIDDEN, response.getStatus());
        }
    }

    @Test
    public void handle_backslashTraversalIsForbidden() throws Exception {
        try (Response response = handler.handle(createSession("..\\outside.txt"))) {
            assertEquals(Response.Status.FORBIDDEN, response.getStatus());
        }
    }

    @Test
    public void handle_siblingWithSharedPrefixIsForbidden() throws Exception {
        try (Response response = handler.handle(createSession("../externalFiles-secret/secret.txt"))) {
            assertEquals(Response.Status.FORBIDDEN, response.getStatus());
        }
    }

    private IHTTPSession createSession(String path) {
        Map<String, List<String>> params = new HashMap<>();
        if (path != null) {
            params.put("path", List.of(path));
        }
        return (IHTTPSession) Proxy.newProxyInstance(
                IHTTPSession.class.getClassLoader(),
                new Class<?>[]{IHTTPSession.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getParameters" -> params;
                    case "toString" -> "MockSession";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == (args != null ? args[0] : null);
                    default -> null;
                });
    }
}
