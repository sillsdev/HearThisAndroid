package Script;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * A simple simulated file system achieved as a dictionary from path to string content
 */
public class TestFileSystem implements IFileSystem {

    HashMap<String, String> files = new HashMap<String, String>();

    public String externalFilesDirectory;

    public String project;

    public String getProjectDirectory() {
        return externalFilesDirectory + "/" + project;
    }
    public String getInfoTxtPath() { return getProjectDirectory() + "/info.txt";}

    @Override
    public boolean FileExists(String path) {
        return files.containsKey(path);
    }

    public void SimulateFile(String path, String content) {
        files.put(path, content);
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
