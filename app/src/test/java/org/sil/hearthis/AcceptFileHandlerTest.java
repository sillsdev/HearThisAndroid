package org.sil.hearthis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
        Map<String, String> parms = new HashMap<>();
        parms.put("path", "ProjectA/info.txt");
        String testContent = "Genesis;10:0";

        IHTTPSession session = createMockSession(parms, "content", testContent);

        try (Response ignored = handler.handle(session)) {
            File savedFile = new File(baseDir, "ProjectA/info.txt");
            assertTrue("File should be saved", savedFile.exists());
            
            byte[] bytes = Files.readAllBytes(savedFile.toPath());
            String savedContent = new String(bytes, StandardCharsets.UTF_8);
            assertEquals(testContent, savedContent);
        }
    }

    @Test
    public void handle_savesPostDataToFile() throws Exception {
        Map<String, String> parms = new HashMap<>();
        parms.put("path", "ProjectA/settings.xml");
        String testContent = "<settings/>";

        // Exercises the fallback logic in AcceptFileHandler when "content" is missing.
        IHTTPSession session = createMockSession(parms, "postData", testContent);

        try (Response ignored = handler.handle(session)) {
            File savedFile = new File(baseDir, "ProjectA/settings.xml");
            assertTrue("File should be saved", savedFile.exists());
            
            byte[] bytes = Files.readAllBytes(savedFile.toPath());
            String savedContent = new String(bytes, StandardCharsets.UTF_8);
            assertEquals(testContent, savedContent);
        }
    }

    @Test
    public void handle_preventsPathTraversal() throws IOException {
        Map<String, String> parms = new HashMap<>();
        parms.put("path", "../secret.txt");
        
        IHTTPSession session = createMockSession(parms, "content", "data");

        try (Response response = handler.handle(session)) {
            assertEquals(Response.Status.FORBIDDEN, response.getStatus());
            File secretFile = new File(baseDir.getParentFile(), "secret.txt");
            assertFalse("File should NOT be saved outside baseDir", secretFile.exists());
        }
    }

    @Test
    public void handle_notifiesListener() throws Exception {
        Map<String, String> parms = new HashMap<>();
        parms.put("path", "test.wav");
        
        IHTTPSession session = createMockSession(parms, "content", "data");

        try (Response ignored = handler.handle(session)) {
            assertEquals("test.wav", mockListener.receivedFileName);
        }
    }

    /**
     * Creates a dynamic proxy for IHTTPSession. This allows us to implement 
     * the modern getParameters() method without writing source code for 
     * the deprecated getParms() method.
     */
    private IHTTPSession createMockSession(Map<String, String> parms, String bodyKey, String bodyValue) {
        return (IHTTPSession) Proxy.newProxyInstance(
                IHTTPSession.class.getClassLoader(),
                new Class<?>[]{IHTTPSession.class},
                (proxy, method, args) -> {
                    String methodName = method.getName();
                    switch (methodName) {
                        case "getParameters" -> {
                            Map<String, List<String>> result = new HashMap<>();
                            parms.forEach((k, v) -> result.put(k, List.of(v)));
                            return result;
                        }
                        case "parseBody" -> {
                            if (args != null && args.length > 0 && args[0] instanceof Map) {
                                // Safe handling of the Map to avoid unchecked cast warnings
                                @SuppressWarnings("unchecked")
                                Map<String, String> files = (Map<String, String>) args[0];
                                files.put(bodyKey, bodyValue);
                            }
                            return null;
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

    private static class TestFileReceivedNotification implements AcceptFileHandler.IFileReceivedNotification {
        String receivedFileName;
        @Override
        public void receivingFile(String name) {
            receivedFileName = name;
        }
    }
}
