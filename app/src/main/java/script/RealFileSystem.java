package script;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Objects;

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
    public void Delete(String path) {File file = new File(path);
        if (file.exists() && !file.delete()) {
            Log.w("RealFileSystem", "Failed to delete file at: " + path);
        }
    }

    @Override
    public ArrayList<String> getDirectories(String path) {
        ArrayList<String> result = new ArrayList<>();
        File directory = new File(path);
        for (File file : Objects.requireNonNull(directory.listFiles())){
            if (file.isDirectory()) {
                result.add(file.getPath());
            }
        }
        return result;
    }
}
