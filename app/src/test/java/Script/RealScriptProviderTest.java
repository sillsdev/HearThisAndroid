package Script;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sil.hearthis.ServiceLocator;

import static org.junit.Assert.*;

public class RealScriptProviderTest {

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testGetLine() throws Exception {

    }

    // Simulated info.txt indicating two books, Genesis and Exodus.
    // Genesis has three chapters of 2, 12, and 25 recordable segments, of which 1, 5, and 12 have been recorded.
    String genEx = "Genesis;2:1,12:5,25:12\nExodus;3:0,10:5";

    @Test
    public void testGetChapter() throws Exception {

    }

    @Test
    public void testGetScriptLineCount() throws Exception {
        RealScriptProvider sp = getGenExScriptProvider();

        assertEquals(12, sp.GetScriptLineCount(0, 1));
        assertEquals(3, sp.GetScriptLineCount(1, 0));
        assertEquals(25, sp.GetScriptLineCount(0, 2));
    }

    private RealScriptProvider getGenExScriptProvider() {
        // Simulate a file system in which the one file is root/test/info.txt containing the genEx data set
        TestFileSystem fs = new TestFileSystem();
        fs.externalFilesDirectory = "root";
        String projName = "test";
        String projPrefix = fs.externalFilesDirectory + "/" + projName;
        fs.SimulateFile(projPrefix + "/info.txt", genEx);

        ServiceLocator.getServiceLocator().setFileSystem(fs);
        return new RealScriptProvider(projPrefix);
    }

    @Test
    public void testGetTranslatedLineCount() throws Exception {
        RealScriptProvider sp = getGenExScriptProvider();

        assertEquals(5, sp.GetTranslatedLineCount(0, 1));
        assertEquals(0, sp.GetTranslatedLineCount(1, 0));
        assertEquals(12, sp.GetTranslatedLineCount(0, 2));

    }

    @Test
    public void testGetTranslatedLineCount1() throws Exception {

    }

    @Test
    public void testGetScriptLineCount1() throws Exception {

    }

    @Test
    public void testLoadBook() throws Exception {

    }

    @Test
    public void testGetEthnologueCode() throws Exception {

    }

    @Test
    public void testNoteBlockRecorded() throws Exception {

    }

    @Test
    public void testGetRecordingFilePath() throws Exception {

    }
}