package Script;

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
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * A simple simulated file system achieved as a dictionary from path to string content
 */
public class TestFileSystem implements IFileSystem {

    HashMap<String, String> files = new HashMap<String, String>();
    HashSet<String> directories = new HashSet<String>();

    public String externalFilesDirectory = "root";

    public String project;

    public String getProjectDirectory() {
        return externalFilesDirectory + "/" + project;
    }
    public String getInfoTxtPath() { return getProjectDirectory() + "/info.txt";}

    public String getDefaultInfoTxtContent() {
        return "Genesis;\n" +
                "Exodus;\n" +
                "Leviticus;\n" +
                "Numbers;\n" +
                "Deuteronomy;\n" +
                "Joshua;\n" +
                "Judges;\n" +
                "Ruth;\n" +
                "1 Samuel;\n" +
                "2 Samuel;\n" +
                "1 Kings;\n" +
                "2 Kings;\n" +
                "1 Chronicles;\n" +
                "2 Chronicles;\n" +
                "Ezra;\n" +
                "Nehemiah;\n" +
                "Esther;\n" +
                "Job;\n" +
                "Psalms;\n" +
                "Proverbs;\n" +
                "Ecclesiastes;\n" +
                "Song of Songs;\n" +
                "Isaiah;\n" +
                "Jeremiah;\n" +
                "Lamentations;\n" +
                "Ezekiel;\n" +
                "Daniel;\n" +
                "Hosea;\n" +
                "Joel;\n" +
                "Amos;\n" +
                "Obadiah;\n" +
                "Jonah;\n" +
                "Micah;\n" +
                "Nahum;\n" +
                "Habakkuk;\n" +
                "Zephaniah;\n" +
                "Haggai;\n" +
                "Zechariah;\n" +
                "Malachi;\n" +
                "Matthew;1:1,12:6,25:12,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0\n" +
                "Mark;\n" +
                "Luke;\n" +
                "John;\n" +
                "Acts;\n" +
                "Romans;\n" +
                "1 Corinthians;\n" +
                "2 Corinthians;\n" +
                "Galatians;\n" +
                "Ephesians;\n" +
                "Philippians;\n" +
                "Colossians;\n" +
                "1 Thessalonians;\n" +
                "2 Thessalonians;\n" +
                "1 Timothy;\n" +
                "2 Timothy;\n" +
                "Titus;\n" +
                "Philemon;\n" +
                "Hebrews;\n" +
                "James;\n" +
                "1 Peter;\n" +
                "2 Peter;\n" +
                "1 John;\n" +
                "2 John;\n" +
                "3 John;\n" +
                "Jude;\n" +
                "Revelation;\n";
    }

    @Override
    public boolean FileExists(String path) {
        return files.containsKey(path);
    }

    public void simulateFile(String path, String content) {
        files.put(path, content);
    }
    public void SimulateDirectory(String path) {
        directories.add(path);
    }

    @Override
    public InputStream ReadFile(String path) throws FileNotFoundException {
        String content = files.get(path);
        // This is not supported by the minimum Android version I'm targeting,
        // but this code only has to work for testing.
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
        ArrayList<String> result = new ArrayList<String>();
        for(String d : directories) {
            if (d.startsWith(path)) {
                // Enhance: if we need to deal with hierarchy, we'll need to find the next slash,
                // truncate to there, and check for duplicates.
                result.add(d);
            }
        }
        return result;
    }

    Element MakeElement(Document doc, Element parent, String name, String content) {
        Element result = doc.createElement(name);
        parent.appendChild(result);
        result.setTextContent(content);
        return result;
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
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }

    class NotifyCloseByteArrayStream extends ByteArrayOutputStream
    {
        TestFileSystem parent;
        String path;

        public NotifyCloseByteArrayStream(String path, TestFileSystem parent) {
            this.path = path;
            this.parent = parent;
        }
        @Override
        public void close() throws IOException {
            super.close(); // officially does nothing, but for consistency.
            parent.WriteStreamClosed(path, this.toString("UTF-8"));
        }
    }
}
