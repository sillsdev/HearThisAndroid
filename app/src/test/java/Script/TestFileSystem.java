package Script;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

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
                "Matthew;0:1,12:6,25:12,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0,0:0\n" +
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

    public void SimulateFile(String path, String content) {
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
        SimulateFile(path, content);
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
