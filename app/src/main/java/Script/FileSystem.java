package Script;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * This class implements a more complete set of FileSystem methods on top of a (real or test)
 * IFileSystem
 */
public class FileSystem implements IFileSystem {

    IFileSystem core;
    public FileSystem(IFileSystem core) {
        this.core = core;
    }

    @Override
    public boolean FileExists(String path) {
        return core.FileExists(path);
    }

    @Override
    public InputStream ReadFile(String path) throws FileNotFoundException {
        return core.ReadFile(path);
    }

    @Override
    public OutputStream WriteFile(String path) throws FileNotFoundException {
        return core.WriteFile(path);
    }

    @Override
    public void Delete(String path) {
        core.Delete(path);
    }

    public String getFile(String path) throws IOException {
        BufferedReader reader = new BufferedReader( new InputStreamReader(ReadFile(path),"UTF-8"));
        StringBuilder stringBuilder = new StringBuilder();
        try {
            String line = reader.readLine();
            String ls = System.getProperty("line.separator");

            if (line != null)
                stringBuilder.append(line);
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(ls);
                stringBuilder.append(line);
            }
        }
        finally {
            reader.close();
        }

        return stringBuilder.toString();
    }

    public void putFile(String path, String content) throws IOException {
        BufferedWriter writer = null;
        try
        {
            writer = new BufferedWriter(new OutputStreamWriter(WriteFile(path),"UTF-8"));
            writer.write(content);
        }
        finally
        {
            if (writer != null)
                writer.close( );
        }
    }
}
