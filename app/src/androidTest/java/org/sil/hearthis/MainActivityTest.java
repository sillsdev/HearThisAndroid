package org.sil.hearthis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;
//import static org.mockito.Mockito.*;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.test.ActivityUnitTestCase;
import android.test.AndroidTestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import Script.FileSystem;
import Script.TestFileSystem;
//import Script.TestFileSystem;
//import org.mockito.Mock;
//import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;

/**
 * Created by Thomson on 5/9/2015.
 */
//@RunWith(MockitoJUnitRunner.class)
//public class MainActivityTest  extends ActivityInstrumentationTestCase2<MainActivity> {
@RunWith(AndroidJUnit4.class)
public class MainActivityTest  extends ActivityInstrumentationTestCase2<MainActivity> {
    MainActivity mainActivity;

//    @Mock
//    Context mMockContext;

    public MainActivityTest() {
        super(MainActivity.class);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        // Injecting the Instrumentation instance is required
        // for your test to run with AndroidJUnitRunner.
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        //Activity mActivity = getActivity();
        //setApplication(new org.sil.hearthis.);
    }

    @Test
    //@UiThreadTest
    public void createMainActivity_withNoScripture_startsNoOtherActivity() throws Exception {
        // Simulates no files at all installed
        TestFileSystem fakeFileSystem = new TestFileSystem();
        ServiceLocator.getServiceLocator().externalFilesDirectory = fakeFileSystem.externalFilesDirectory;
        ServiceLocator.getServiceLocator().setFileSystem(new FileSystem(fakeFileSystem));
        //Instrumentation.ActivityMonitor activityMonitor = getInstrumentation().addMonitor(ChooseBookActivity.class.getName(), null, false);
//        Intent mLaunchIntent = new Intent(mMockContext, MainActivity.class);
//        mainActivity = startActivity(mLaunchIntent, null, null);

        // Watch to see whether some other activity is incorrectly started (e.g., recored activity
        // will eventually be started if there is a remembered state; book chooser if there is
        // no remembered state but scripture is loaded.
        // We'd like to watch for ANY activity to be started, but this does not seem to be possible.
        Instrumentation.ActivityMonitor bookChooserMonitor = getInstrumentation().addMonitor(ChooseBookActivity.class.getName(), null, false);
        Instrumentation.ActivityMonitor recordMonitor = getInstrumentation().addMonitor(RecordActivity.class.getName(), null, false);

        //this sems to actually start the activity.
        Activity activity = getActivity();
        assertNotNull(activity);

        // Waiting for idle does not seem to be necessary (see createMainActivity_withScripture_NoSavedLocation_startsChooseBook,
        // which detects that ChooseBookActivity has been launched without it). I'd prefer to
        // have it but can't figure out what 'runnable' to pass to it.
        //getInstrumentation().waitForIdle();
        assertEquals("unexpectedly launched choose book activity", 0, bookChooserMonitor.getHits());
        assertEquals("unexpectedly launched record activity", 0, recordMonitor.getHits());
        //Activity nextActivity = getInstrumentation().waitForMonitorWithTimeout(activityMonitor, 20000);
        //assertNotNull(nextActivity);
        //nextActivity.finish();

//        Intent intent = getStartedActivityIntent();
//        assertNotNull(intent);
    }

    @Test
    //@UiThreadTest
    public void createMainActivity_withScripture_NoSavedLocation_startsChooseBook() throws Exception {
        // Simulates a minimal single scripture instance installed.
        TestFileSystem fakeFileSystem = new TestFileSystem();
        fakeFileSystem.project = "kal";
        String infoPath = fakeFileSystem.getInfoTxtPath();
        fakeFileSystem.SimulateFile(infoPath, fakeFileSystem.getDefaultInfoTxtContent());
        fakeFileSystem.SimulateDirectory(fakeFileSystem.getProjectDirectory());
        ServiceLocator.getServiceLocator().externalFilesDirectory = fakeFileSystem.externalFilesDirectory;
        ServiceLocator.getServiceLocator().setFileSystem(new FileSystem(fakeFileSystem));
        TestScriptProvider sp = new TestScriptProvider();
        ServiceLocator.getServiceLocator().setScriptProvider(sp);

        // watch for the book chooser activity (or, incorrectly, a RecordActivity) to be started.
        // We'd like to watch for ANY activity to be started, but this does not seem to be possible.
        Instrumentation.ActivityMonitor bookChooserMonitor = getInstrumentation().addMonitor(ChooseBookActivity.class.getName(), null, false);
        Instrumentation.ActivityMonitor recordMonitor = getInstrumentation().addMonitor(RecordActivity.class.getName(), null, false);

        // This seems to trigger creation of the main activity, which (with Scripture existing, but no saved selection)
        // should launch the book chooser.
        Activity activity = getActivity();
        assertNotNull(activity);

        assertEquals("did not automatically launch choose book activity", 1, bookChooserMonitor.getHits());
        assertEquals("unexpectedly launched record activity", 0, recordMonitor.getHits());
    }

}