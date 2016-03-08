package org.sil.hearthis;

import android.app.Activity;

import java.io.File;
import java.util.ArrayList;

import Script.FileSystem;
import Script.IFileSystem;
import Script.IScriptProvider;
import Script.Project;
import Script.RealFileSystem;
import Script.RealScriptProvider;

/**
 * This class facilitates locating the instance that should be used of various services.
 * This helps with dependency injection, in particular for the many objects that
 * need access to things that are usually singletons, but which we may wish to mock
 * for testing.
 */
public class ServiceLocator {
    String externalFilesDirectory;
    private IScriptProvider scriptProvider;
    private FileSystem fileSystem;
    private Project project;
    static ServiceLocator theOneInstance = new ServiceLocator();

    // When you need the service locator call this to get it.
    public static ServiceLocator getServiceLocator() {return theOneInstance;}

    // Each activity should call this from its create method. The first such call will
    // initialize various things. Test code may instead install various stubs.
    // Returns this for convenient chaining.
    public ServiceLocator init(Activity activity) {
        if (externalFilesDirectory == null)
            externalFilesDirectory = activity.getExternalFilesDir(null).toString();
        return this;
    }

    // Init function only for testing.
    public ServiceLocator testInit(String exFileDir) {
        externalFilesDirectory = exFileDir;
        return this;
    }

    public FileSystem getFileSystem() {
        if (fileSystem == null)
            fileSystem = new FileSystem(new RealFileSystem());
        return fileSystem;
    }
    public void setFileSystem(FileSystem fs) {
        fileSystem = fs;
    }

    public IScriptProvider getScriptProvider() {
        if (scriptProvider == null) {
            // Todo: Dhh is just for testing, eventually we have to pick a project if there is
            // more than one, do something appropriate if there are none. Maybe go right to sync?
            // Todo: scan org.sil.hearthis/files for folders containing info.txt and open first
            // Todo: remember last project
            // Todo: if no real project available use SampleScriptProvider.
            ArrayList<String> rootDirs = getFileSystem().getDirectories(externalFilesDirectory);
            if (rootDirs.isEmpty())
                return null; // we can't get a script provider if we have no scripture on file.
            scriptProvider = new RealScriptProvider(rootDirs.get(0));
        }
        return scriptProvider;
    }
    public void setScriptProvider(IScriptProvider sp) {
        scriptProvider = sp;
    }

    public Project getProject() {
        if (project == null)
            project = new Project(getScriptProvider().getProjectName(), getScriptProvider());
        return project;
    }
}
