package Script;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Implement interface to talk to the real file system on the device.
 */
public class RealFileSystem implements IFileSystem {
    @Override
    public boolean FileExists(String path) {
        return new File(path).exists();
    }

    @Override
    public InputStream ReadFile(String path) throws FileNotFoundException {
        return new FileInputStream(path);
    }

    @Override
    public OutputStream WriteFile(String path) throws FileNotFoundException{
        return new FileOutputStream(path);
    }

    @Override
    public void Delete(String path) {
        new File(path).delete();
    }

    @Override
    public ArrayList<String> getDirectories(String path) {
        ArrayList<String> result = new ArrayList<String>();
        File directory = new File(path);
        for (File file : directory.listFiles()){
            if (file.isDirectory()) {
                result.add(file.getPath());
            }
        }
        return result;
    }
}
