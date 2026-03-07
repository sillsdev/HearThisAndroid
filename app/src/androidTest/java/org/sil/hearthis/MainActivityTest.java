package org.sil.hearthis;

import static org.junit.Assert.assertEquals;

import android.app.Instrumentation;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import script.FileSystem;
import script.TestFileSystem;

/**
 * Modernized MainActivityTest using ActivityScenario.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Before
    public void setUp() {
        // Reset ServiceLocator for each test to ensure a clean state
        ServiceLocator.theOneInstance = new ServiceLocator();
    }

    @Test
    public void createMainActivity_withNoScripture_startsNoOtherActivity() {
        // Simulates no files at all installed
        TestFileSystem fakeFileSystem = new TestFileSystem();
        ServiceLocator.getServiceLocator().externalFilesDirectory = fakeFileSystem.externalFilesDirectory;
        ServiceLocator.getServiceLocator().setFileSystem(new FileSystem(fakeFileSystem));

        Instrumentation.ActivityMonitor bookChooserMonitor = InstrumentationRegistry.getInstrumentation()
                .addMonitor(ChooseBookActivity.class.getName(), null, false);
        Instrumentation.ActivityMonitor recordMonitor = InstrumentationRegistry.getInstrumentation()
                .addMonitor(RecordActivity.class.getName(), null, false);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(Assert::assertNotNull);

            assertEquals("unexpectedly launched choose book activity", 0, bookChooserMonitor.getHits());
            assertEquals("unexpectedly launched record activity", 0, recordMonitor.getHits());
        }
    }

    @Test
    public void createMainActivity_withScripture_NoSavedLocation_startsChooseBook() {
        // Simulates a minimal single scripture instance installed.
        TestFileSystem fakeFileSystem = new TestFileSystem();
        fakeFileSystem.project = "kal";
        String infoPath = fakeFileSystem.getInfoTxtPath();
        fakeFileSystem.simulateFile(infoPath, fakeFileSystem.getDefaultInfoTxtContent());
        fakeFileSystem.SimulateDirectory(fakeFileSystem.getProjectDirectory());
        
        final ServiceLocator serviceLocator = ServiceLocator.getServiceLocator();
        serviceLocator.externalFilesDirectory = fakeFileSystem.externalFilesDirectory;
        serviceLocator.setFileSystem(new FileSystem(fakeFileSystem));

        Instrumentation.ActivityMonitor bookChooserMonitor = InstrumentationRegistry.getInstrumentation()
                .addMonitor(ChooseBookActivity.class.getName(), null, false);
        Instrumentation.ActivityMonitor recordMonitor = InstrumentationRegistry.getInstrumentation()
                .addMonitor(RecordActivity.class.getName(), null, false);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(Assert::assertNotNull);

            // The activity might take a moment to launch the next one
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            assertEquals("did not automatically launch choose book activity", 1, bookChooserMonitor.getHits());
            assertEquals("unexpectedly launched record activity", 0, recordMonitor.getHits());

            // We use 39 because Matthew is currently the only book that has chapters in the default test info.txt
            // Note: Updated extension to .wav to match current RecordActivity.useWaveRecorder = true
            assertEquals("Should find info.txt and set name of Scripture from it", 
                    "root/kal/Matthew/1/2.wav",
                    serviceLocator.getScriptProvider().getRecordingFilePath(39, 1, 2));
        }
    }
}
