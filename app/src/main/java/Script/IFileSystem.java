package Script;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * This interface 'wraps' the functionality we need from the file system to facilitate unit testing.
 */
public interface IFileSystem {
    boolean FileExists(String path);
    InputStream ReadFile(String path) throws FileNotFoundException;
    OutputStream WriteFile(String path) throws FileNotFoundException;
    void Delete(String path);
    ArrayList<String> getDirectories(String path);
}
