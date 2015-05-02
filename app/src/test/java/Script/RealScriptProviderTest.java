package Script;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sil.hearthis.ServiceLocator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.Assert.*;

public class RealScriptProviderTest {

    private TestFileSystem fs;

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
        fs = new TestFileSystem();
        fs.externalFilesDirectory = "root";
        fs.project = "test";
        fs.SimulateFile(fs.getInfoTxtPath(), genEx);

        ServiceLocator.getServiceLocator().setFileSystem(new FileSystem(fs));
        return new RealScriptProvider(fs.getProjectDirectory());
    }

    String ex0 = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
        "<ChapterInfo Number=\"0\">\n" +
            "<Source>" +
                "<ScriptLine><LineNumber>1</LineNumber><Text>Some Introduction Header</Text><Heading>true</Heading></ScriptLine>" +
                "<ScriptLine><LineNumber>2</LineNumber><Text>Some Introduction First</Text><Heading>true</Heading></ScriptLine>" +
                "<ScriptLine><LineNumber>3</LineNumber><Text>Some Introduction Second</Text><Heading>true</Heading></ScriptLine>" +
            "</Source>" +
        "</ChapterInfo>";

    private void addEx0Chapter(TestFileSystem fs) {
        String path = getEx0Path(fs);
        fs.SimulateFile(path, ex0);
    }

    private String getEx0Path(TestFileSystem fs) {
        return fs.getProjectDirectory() + "/" + "Exodus/0/info.xml";
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
    public void testNoteBlockRecorded_NothingRecorded_AddsRecording() throws Exception {
        RealScriptProvider sp = getGenExScriptProvider();
        addEx0Chapter(fs);
        sp.noteBlockRecorded(1, 0, 2);
        Element recording = findOneElementByTagName(fs.ReadFile(getEx0Path(fs)), "Recordings");
        Element line = findNthChild(recording, 0, 1, "ScriptLine");
        verifyChildContent(line, "LineNumber", "3");
        verifyChildContent(line, "Text", "Some Introduction Second");
        verifyRecordingCount(1, 0, 1);
    }

    void verifyRecordingCount(int bookNum, int chapNum, int count)
    {
        String infoTxt = fs.getFile(fs.getInfoTxtPath());
        String[] lines = infoTxt.split("\\n");
        assertTrue("not enough lines in infoTxt", lines.length >= bookNum);
        String bookLine = lines[bookNum]; // Like Exodus;3:0,10:5
        String[] counts = bookLine.split(";")[1].split(",");
        assertTrue("not enough chapters in counts", counts.length >= chapNum);
        String chapData = counts[chapNum];
        String recCount = chapData.split(":")[1];
        int recordings = Integer.parseInt(recCount);
        assertEquals("wrong number of recordings", count, recordings);
    }

    @Test
    public void testNoteBlockRecorded_LaterRecorded_AddsRecordingBefore() throws Exception {
        RealScriptProvider sp = getGenExScriptProvider();
        addEx0Chapter(fs);
        sp.noteBlockRecorded(1, 0, 2);
        sp.noteBlockRecorded(1,0, 1);
        Element recording = findOneElementByTagName(fs.ReadFile(getEx0Path(fs)), "Recordings");
        Element line = findNthChild(recording, 0, 2, "ScriptLine");
        verifyChildContent(line, "LineNumber", "2");
        verifyChildContent(line, "Text", "Some Introduction First");
        line = findNthChild(recording, 1, 2, "ScriptLine");
        verifyChildContent(line, "LineNumber", "3");
        verifyChildContent(line, "Text", "Some Introduction Second");
    }

    @Test
    public void testNoteBlockRecorded_EarlierRecorded_AddsRecordingAfter() throws Exception {
        RealScriptProvider sp = getGenExScriptProvider();
        addEx0Chapter(fs);
        sp.noteBlockRecorded(1, 0, 1);
        sp.noteBlockRecorded(1,0, 2);
        Element recording = findOneElementByTagName(fs.ReadFile(getEx0Path(fs)), "Recordings");
        Element line = findNthChild(recording, 0, 2, "ScriptLine");
        verifyChildContent(line, "LineNumber", "2");
        verifyChildContent(line, "Text", "Some Introduction First");
        line = findNthChild(recording, 1, 2, "ScriptLine");
        verifyChildContent(line, "LineNumber", "3");
        verifyChildContent(line, "Text", "Some Introduction Second");
    }

    @Test
    public void testNoteBlockRecorded_RecordSame_Overwrites() throws Exception {
        RealScriptProvider sp = getGenExScriptProvider();
        addEx0Chapter(fs);
        sp.noteBlockRecorded(1, 0, 1);
        String ex0Path = getEx0Path(fs);
        String original = fs.getFile(ex0Path);
        String updated = original.replace("Some Introduction First", "New Introduction");
        fs.SimulateFile(ex0Path, updated);

        sp.noteBlockRecorded(1, 0, 1); // should overwrite

        Element recording = findOneElementByTagName(fs.ReadFile(ex0Path), "Recordings");
        Element line = findNthChild(recording, 0, 1, "ScriptLine");
        verifyChildContent(line, "LineNumber", "2");
        verifyChildContent(line, "Text", "New Introduction");
    }

    @Test
    public void testGetRecordingFilePath() throws Exception {

    }

    // Read input as an XML document. Verify that getElementsByTagName(tag) yields exactly one element
    // and return it.
    Element findOneElementByTagName(InputStream input, String tag) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document dom = builder.parse(input);
            Element root = dom.getDocumentElement();
            NodeList source = root.getElementsByTagName(tag);
            assertEquals("Did not find expected number of elements with tag " + tag, 1, source.getLength());
            Node node = source.item(0);
            assertTrue("expected match to be an Element", node instanceof Element);
            return (Element) node;
        }
        catch(Exception ex) {
            assertTrue("Unexpected exception in findOneElementMatching " + ex.toString(), ex == null);
        }
        return null; // unreachable
    }

    // Verify that parent has count children and the indexth one has the specified tag.
    // return the indexth element.
    Element findNthChild(Element parent, int index, int count, String tag) {
        assertEquals(count, parent.getChildNodes().getLength());
        Node nth = parent.getChildNodes().item(index);
        assertTrue("expected nth child to be Element", nth instanceof Element);
        Element result = (Element) nth;
        assertEquals(tag, result.getTagName());
        return result;
    }

    // Verify that parent has exactly one child with the specified tag, and its content is as specified.
    void verifyChildContent(Element parent, String tag, String content) {
        NodeList children = parent.getElementsByTagName(tag);
        assertEquals(1, children.getLength());
        Node child = children.item(0);
        assertTrue("expected child to be Element", child instanceof Element);
        Element elt = (Element) child;
        assertEquals(content, elt.getTextContent());
    }
}