package org.sil.hearthis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;

import fi.iki.elonen.NanoHTTPD.Response;

/**
 * Unit tests for the shared path-traversal check.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class PathGuardTest {

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    private File baseDir;

    @Before
    public void setUp() throws IOException {
        baseDir = tempFolder.newFolder("externalFiles");
        // Shares baseDir's name as a prefix, so a bare startsWith check would accept it.
        tempFolder.newFolder("externalFiles-secret");
        assertTrue(new File(baseDir, "ProjectA").mkdir());
    }

    private Response check(String path, boolean allowBase) {
        return PathGuard.check(baseDir, new File(baseDir, path), path, allowBase);
    }

    private void assertForbidden(String path, boolean allowBase) {
        try (Response r = check(path, allowBase)) {
            assertEquals(Response.Status.FORBIDDEN, r.getStatus());
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void childIsAllowed() {
        assertNull(check("ProjectA/file.txt", false));
        assertNull(check("ProjectA/file.txt", true));
    }

    @Test
    public void nonExistentChildIsAllowed() {
        assertNull(check("New/Deep/file.txt", false));
    }

    @Test
    public void dotDotThatStaysInsideIsAllowed() {
        assertNull(check("ProjectA/../ProjectA/file.txt", false));
    }

    @Test
    public void baseDirAllowedOnlyWhenRequested() {
        assertNull(check("", true));
        assertNull(check(".", true));
        assertNull(check("ProjectA/..", true));
        assertForbidden("", false);
        assertForbidden(".", false);
        assertForbidden("ProjectA/..", false);
    }

    @Test
    public void parentTraversalIsForbidden() {
        assertForbidden("..", true);
        assertForbidden("../", true);
        assertForbidden("../x.txt", false);
        assertForbidden("ProjectA/../../x.txt", false);
    }

    @Test
    public void siblingWithSharedPrefixIsForbidden() {
        assertForbidden("../externalFiles-secret", true);
        assertForbidden("../externalFiles-secret/secret.txt", false);
    }
}
