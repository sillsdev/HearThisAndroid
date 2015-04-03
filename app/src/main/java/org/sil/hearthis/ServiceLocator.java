package org.sil.hearthis;

import android.app.Activity;

import java.io.File;

import Script.IFileSystem;
import Script.IScriptProvider;
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
    IScriptProvider scriptProvider;
    IFileSystem fileSystem;
    static ServiceLocator theOneInstance = new ServiceLocator();

    // When you need the service locator call this to get it.
    public static ServiceLocator getServiceLocator() {return theOneInstance;}

    // Each activity should call this from its create method. The first such call will
    // initialize various things. Test code may instead install various stubs.
    // Returns this for convenient chaining.
    public ServiceLocator init(Activity activity) {
        externalFilesDirectory = activity.getExternalFilesDir(null).toString();
        return this;
    }

    // Init function only for testing.
    public ServiceLocator testInit(String exFileDir) {
        externalFilesDirectory = exFileDir;
        return this;
    }

    public IFileSystem getFileSystem() {
        if (fileSystem == null)
            fileSystem = new RealFileSystem();
        return fileSystem;
    }
    public void setFileSystem(IFileSystem fs) {
        fileSystem = fs;
    }

    public IScriptProvider getScriptProvider() {
        if (scriptProvider == null) {
            // Todo: Dhh is just for testing, eventually we have to pick a project if there is
            // more than one, do something appropriate if there are none. Maybe go right to sync?
            // Todo: scan org.sil.hearthis/files for folders containing info.txt and open first
            // Todo: remember last project
            // Todo: if no real project available use SampleScriptProvider.
            scriptProvider = new RealScriptProvider(externalFilesDirectory + "/" + "Dhh");
        }
        return scriptProvider;
    }
    public void setScriptProvider(IScriptProvider sp) {
        scriptProvider = sp;
    }
}
