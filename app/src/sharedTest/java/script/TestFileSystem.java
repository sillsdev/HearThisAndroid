package script;

import android.os.Build;
import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * A simple simulated file system achieved as a dictionary from path to string content
 */
public class TestFileSystem implements IFileSystem {

    final HashMap<String, String> files = new HashMap<>();
    final HashSet<String> directories = new HashSet<>();

    public String externalFilesDirectory = "root";

    public String project;

    public String getProjectDirectory() {
        return externalFilesDirectory + "/" + project;
    }
    public String getInfoTxtPath() { return getProjectDirectory() + "/info.txt";}

    public String getDefaultInfoTxtContent() {
        return """
                Genesis;
                Exodus;
                Leviticus;
                Numbers;
                Deuteronomy;
                Joshua;
                Judges;
                Ruth;
                1 Samuel;
                2 Samuel;
                1 Kings;
                2 Kings;
                1 Chronicles;
                2 Chronicles;
                Ezra;
                Nehemiah;
                Esther;
                Job;
                Psalms;
                Proverbs;
                Ecclesiastes;
                Song of Songs;
                Isaiah;
                Jeremiah;
                Lamentations;
                Ezekiel;
                Daniel;
                Hosea;
                Joel;
                Amos;
                Obadiah;
                Jonah;
                Micah;
                Nahum;
                Habakkuk;
                Zephaniah;
                Haggai;
                Zechariah;
                Malachi;
                Matthew;1:1,12:6,25:12,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0
                Mark;
                Luke;
                John;
                Acts;
                Romans;
                1 Corinthians;
                2 Corinthians;
                Galatians;
                Ephesians;
                Philippians;
                Colossians;
                1 Thessalonians;
                2 Thessalonians;
                1 Timothy;
                2 Timothy;
                Titus;
                Philemon;
                Hebrews;
                James;
                1 Peter;
                2 Peter;
                1 John;
                2 John;
                3 John;
                Jude;
                Revelation;
                """;
    }

    @Override
    public boolean FileExists(String path) {
        return files.containsKey(path);
    }

    public void simulateFile(String path, String content) {
        files.put(path, content);
    }
    @SuppressWarnings("unused")
    // Method is used in another file
    public void SimulateDirectory(String path) {
        directories.add(path);
    }

    @Override
    public InputStream ReadFile(String path) {
        String content = files.get(path);
        // This is not supported by the minimum Android version I'm targeting,
        // but this code only has to work for testing.
        assert content != null;
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    public String getFile(String path) {
        return files.get(path);
    }

    @Override
    public OutputStream WriteFile(String path) throws FileNotFoundException {
        return new NotifyCloseByteArrayStream(path, this);
    }

    public void WriteStreamClosed(String path, String content) {
        simulateFile(path, content);
    }

    @Override
    public void Delete(String path) {
        files.remove(path);
    }

    @Override
    public ArrayList<String> getDirectories(String path) {
        ArrayList<String> result = new ArrayList<>();
        for(String d : directories) {
            if (d.startsWith(path)) {
                // Enhance: if we need to deal with hierarchy, we'll need to find the next slash,
                // truncate to there, and check for duplicates.
                result.add(d);
            }
        }
        return result;
    }

    void MakeElement(Document doc, Element parent, String name, String content) {
        Element result = doc.createElement(name);
        parent.appendChild(result);
        result.setTextContent(content);
        }

    // Make a simulated info.txt file for the specified chapter. Contents are the specified lines.
    // It also has recording elements for those recordingTexts which are non-null. It is OK to have
    // fewer recordingTexts than lines, or even to pass null, in which case there will be no
    // recordings element.
    public void MakeChapterContent(String bookName, int chapNum, String[] lines, String[] recordingTexts) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element root = doc.createElement("ChapterInfo");
            root.setAttribute("Number", Integer.toString(chapNum));
            doc.appendChild(root);

            if (recordingTexts != null) {
                Element recordings = doc.createElement("Recordings");
                root.appendChild(recordings);
                for (int iline = 0; iline < recordingTexts.length; iline++) {
                    if (recordingTexts[iline] != null) {
                        Element line = doc.createElement("ScriptLine");
                        recordings.appendChild(line);
                        MakeElement(doc, line, "LineNumber", Integer.toString(iline + 1));
                        MakeElement(doc, line, "Text", recordingTexts[iline]);
                    }
                }
            }

            Element source = doc.createElement("Source");
            root.appendChild(source);

            for (int iline = 0; iline < lines.length; iline++) {
                Element line = doc.createElement("ScriptLine");
                source.appendChild(line);
                MakeElement(doc, line, "LineNumber", Integer.toString(iline+1));
                MakeElement(doc, line, "Text", lines[iline]);
            }
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource domSource = new DOMSource(root);
            OutputStream fos = WriteFile(project + "/" + bookName + "/" + chapNum + "/" + RealScriptProvider.infoFileName);
            StreamResult streamResult = new StreamResult(fos);
            transformer.transform(domSource, streamResult);
            fos.flush();
            fos.close();
        } catch (ParserConfigurationException | IOException | TransformerException e) {
            Log.e("TestFileSystem", "Error creating chapter content", e);
        }
    }

    static class NotifyCloseByteArrayStream extends ByteArrayOutputStream
    {
        final TestFileSystem parent;
        final String path;

        public NotifyCloseByteArrayStream(String path, TestFileSystem parent) {
            this.path = path;
            this.parent = parent;
        }
        @Override
        public void close() throws IOException {
            super.close(); // officially does nothing, but for consistency.
            String content;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                content = this.toString(StandardCharsets.UTF_8);
            } else {
                //noinspection CharsetObjectCanBeUsed
                content = this.toString("UTF-8");
            }
            parent.WriteStreamClosed(path, content);
        }
    }
}
