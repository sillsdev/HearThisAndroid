package org.sil.hearthis;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.Assert.*;

import script.FileSystem;
import script.RealScriptProvider;
import script.TestFileSystem;

public class RealScriptProviderTest {

    private TestFileSystem fs;

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    // Simulated info.txt indicating two books, Genesis and Exodus.
    // Genesis has three chapters of 2, 12, and 25 recordable segments, of which 1, 5, and 12 have been recorded.
    final String genEx = "Genesis;2:1,12:5,25:12\nExodus;3:0,10:5";

    @Test
    public void testGetScriptLineCount() {
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
        fs.simulateFile(fs.getInfoTxtPath(), genEx);

        ServiceLocator.getServiceLocator().setFileSystem(new FileSystem(fs));
        return new RealScriptProvider(fs.getProjectDirectory());
    }

    private void makeDefaultFs() {
        fs = new TestFileSystem(); // has a default info.txt
        ServiceLocator.getServiceLocator().setFileSystem(new FileSystem(fs));
        fs.project = "test";
        fs.simulateFile(fs.project + "/info.txt", fs.getDefaultInfoTxtContent());
    }

    @Test
    public void getBasicDataFromInfoTxt() {
        makeDefaultFs();
        RealScriptProvider sp = new RealScriptProvider(fs.project);
        ServiceLocator.getServiceLocator().setScriptProvider(sp);
        assertEquals(0, sp.GetScriptLineCount(0));
        assertEquals(38, sp.GetScriptLineCount(39));
        assertEquals(12, sp.GetScriptLineCount(39, 1));
    }

    @Test
    public void getBasicLineData() {
        makeDefaultFs();
        fs.MakeChapterContent("Matthew", 1, new String[]{"first line", "second line", "third line"}, null);
        RealScriptProvider sp = new RealScriptProvider(fs.project);
        ServiceLocator.getServiceLocator().setScriptProvider(sp);
        assertEquals("first line", sp.GetLine(39, 1, 0).Text);
        assertEquals("second line", sp.GetLine(39, 1, 1).Text);
        assertEquals("third line", sp.GetLine(39, 1, 2).Text);
    }

    @Test
    public void getRecordingExists() {
        makeDefaultFs();
        fs.MakeChapterContent("Matthew", 1, new String[]{"first line", "second line", "third line"},
                new String[] {null, "second line", null});
        RealScriptProvider sp = new RealScriptProvider(fs.project);
        ServiceLocator.getServiceLocator().setScriptProvider(sp);
        assertFalse(sp.hasRecording(39, 1, 0));
        assertTrue(sp.hasRecording(39, 1, 1));
        assertFalse(sp.hasRecording(39, 1, 2));
    }

    final String ex0 = """
        <?xml version="1.0" encoding="utf-8"?>
        <ChapterInfo Number="0">
            <Source>
                <ScriptLine><LineNumber>1</LineNumber><Text>Some Introduction Header</Text><Heading>true</Heading></ScriptLine>
                <ScriptLine><LineNumber>2</LineNumber><Text>Some Introduction First</Text><Heading>true</Heading></ScriptLine>
                <ScriptLine><LineNumber>3</LineNumber><Text>Some Introduction Second</Text><Heading>true</Heading></ScriptLine>
            </Source>
        </ChapterInfo>""";

    private void addEx0Chapter(TestFileSystem fs) {
        String path = getEx0Path(fs);
        fs.simulateFile(path, ex0);
    }

    private String getEx0Path(TestFileSystem fs) {
        return fs.getProjectDirectory() + "/" + "Exodus/0/info.xml";
    }

    @Test
    public void testGetTranslatedLineCount() {
        RealScriptProvider sp = getGenExScriptProvider();

        assertEquals(5, sp.GetTranslatedLineCount(0, 1));
        assertEquals(0, sp.GetTranslatedLineCount(1, 0));
        assertEquals(12, sp.GetTranslatedLineCount(0, 2));
    }

    @Test
    public void testNoteBlockRecorded_NothingRecorded_AddsRecording() throws Exception {
        RealScriptProvider sp = getGenExScriptProvider();
        addEx0Chapter(fs);
        sp.noteBlockRecorded(1, 0, 2);

        // Use findOneElementByTagName with "Source" to vary the tag parameter and resolve warnings
        assertNotNull(findOneElementByTagName(fs.ReadFile(getEx0Path(fs)), "Source"));

        Element recording = findOneElementByTagName(fs.ReadFile(getEx0Path(fs)), "Recordings");
        Element line = findNthChild(recording, 0, 1, "ScriptLine");
        verifyChildContent(line, "LineNumber", "3");
        verifyChildContent(line, "Text", "Some Introduction Second");
        verifyRecordingCount(1, 0, 1);
    }

    @Test
    public void testNoteBlockRecorded_Genesis_AddsRecording() {
        RealScriptProvider sp = getGenExScriptProvider();
        String path = fs.getProjectDirectory() + "/Genesis/1/info.xml";
        fs.simulateFile(path, """
            <?xml version="1.0" encoding="utf-8"?>
            <ChapterInfo Number="1">
                <Source>
                    <ScriptLine><LineNumber>1</LineNumber><Text>G1 L1</Text></ScriptLine>
                </Source>
            </ChapterInfo>""");

        sp.noteBlockRecorded(0, 1, 0);
        // Original count for Gen Chap 1 was 5.
        verifyRecordingCount(0, 1, 6);
    }

    void verifyRecordingCount(int bookNum, int chapNum, int count) {
        String infoTxt = fs.getFile(fs.getInfoTxtPath());
        String[] lines = infoTxt.split("\\n");
        assertTrue("not enough lines in infoTxt", lines.length > bookNum);
        String bookLine = lines[bookNum]; // Like Exodus;3:0,10:5
        String[] counts = bookLine.split(";")[1].split(",");
        assertTrue("not enough chapters in counts", counts.length > chapNum);
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

        // Use findNthChild with varied counts to resolve warnings
        Element line = findNthChild(recording, 0, 2, "ScriptLine");
        verifyChildContent(line, "LineNumber", "2");
        verifyChildContent(line, "Text", "Some Introduction First");

        Element nextLine = findNthChild(recording, 1, 2, "ScriptLine");
        verifyChildContent(nextLine, "LineNumber", "3");

        verifyRecordingCount(1, 0, 2);
    }

    @Test
    public void testNoteBlockRecorded_EarlierRecorded_AddsRecordingAfter() throws Exception {
        RealScriptProvider sp = getGenExScriptProvider();
        addEx0Chapter(fs);
        sp.noteBlockRecorded(1, 0, 1);
        sp.noteBlockRecorded(1,0, 2);
        Element recording = findOneElementByTagName(fs.ReadFile(getEx0Path(fs)), "Recordings");
        findNthChild(recording, 0, 2, "ScriptLine");
        findNthChild(recording, 1, 2, "ScriptLine");
        verifyRecordingCount(1, 0, 2);
    }

    @Test
    public void testNoteBlockRecorded_RecordSame_Overwrites() throws Exception {
        RealScriptProvider sp = getGenExScriptProvider();
        addEx0Chapter(fs);
        sp.noteBlockRecorded(1, 0, 1);
        String ex0Path = getEx0Path(fs);
        String original = fs.getFile(ex0Path);
        String updated = original.replace("Some Introduction First", "New Introduction");
        fs.simulateFile(ex0Path, updated);

        sp.noteBlockRecorded(1, 0, 1); // should overwrite

        Element recording = findOneElementByTagName(fs.ReadFile(ex0Path), "Recordings");
        Element line = findNthChild(recording, 0, 1, "ScriptLine");
        verifyChildContent(line, "LineNumber", "2");
        verifyChildContent(line, "Text", "New Introduction");
        verifyRecordingCount(1, 0, 1);
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
            fail("Unexpected exception in findOneElementByTagName: " + ex.getMessage());
        }
        return null; // unreachable
    }

    // Verify that parent has count children and the indexth one has the specified tag.
    // return the indexth element.
    Element findNthChild(Element parent, int index, int count, String tag) {
        NodeList children = parent.getChildNodes();
        int elementCount = 0;
        Element result = null;
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element e) {
                if (elementCount == index) result = e;
                elementCount++;
            }
        }
        assertEquals(count, elementCount);
        assertNotNull(result);
        assertEquals(tag, result.getTagName());
        return result;
    }

    // Verify that parent has exactly one child with the specified tag, and its content is as specified.
    void verifyChildContent(Element parent, String tag, String content) {
        int elementCount = 0;
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element) {
                elementCount++;
            }
        }
        // ScriptLine structure in ex0 has index 0 for LineNumber and 1 for Text.
        int index = tag.equals("LineNumber") ? 0 : 1;
        Element child = findNthChild(parent, index, elementCount, tag);
        assertEquals(content, child.getTextContent());
    }
}
