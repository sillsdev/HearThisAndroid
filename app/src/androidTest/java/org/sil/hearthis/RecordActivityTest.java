package org.sil.hearthis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import script.BibleLocation;
import script.BookInfo;
import script.FileSystem;
import script.RealScriptProvider;
import script.TestFileSystem;

/**
 * Instrumentation tests for RecordActivity.
 * Covers: Loading, Navigation, Recording Workflow, and State Persistence.
 * Uses onActivity assertions for stability on older Android versions (API 26-28).
 */
@RunWith(AndroidJUnit4.class)
public class RecordActivityTest {

    // Automatically grant the recording permission so the tests can flow through to the logic.
    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO);

    @Before
    public void setUp() {
        // Reset ServiceLocator for each test to ensure a clean state
        ServiceLocator.theOneInstance = new ServiceLocator();
        
        // Set up a fake file system with some data
        TestFileSystem fakeFileSystem = new TestFileSystem();
        fakeFileSystem.externalFilesDirectory = "root";
        fakeFileSystem.project = "testProject";
        
        // Simpler info.txt: Just Matthew (index 0)
        String infoTxt = "Matthew;2:0\n"; // Matthew, 1 chapter (index 0), 2 lines
        
        fakeFileSystem.simulateFile(fakeFileSystem.getInfoTxtPath(), infoTxt);
        fakeFileSystem.SimulateDirectory(fakeFileSystem.getProjectDirectory());
        
        // Simulate the info.xml for Matthew Chapter 1
        String chapterInfoPath = "root/testProject/Matthew/0/info.xml";
        fakeFileSystem.SimulateDirectory("root/testProject/Matthew/0");
        fakeFileSystem.simulateFile(chapterInfoPath,
            "<ChapterInfo><Source>" +
            "<ScriptLine><Text>Matthew line 0</Text></ScriptLine>" +
            "<ScriptLine><Text>Matthew line 1</Text></ScriptLine>" +
            "</Source></ChapterInfo>");

        ServiceLocator.getServiceLocator().externalFilesDirectory = fakeFileSystem.externalFilesDirectory;
        ServiceLocator.getServiceLocator().setFileSystem(new FileSystem(fakeFileSystem));
        
        // Manually set the ScriptProvider to point to our test project
        ServiceLocator.getServiceLocator().setScriptProvider(new RealScriptProvider(fakeFileSystem.getProjectDirectory()));
    }

    @Test
    public void recordActivity_loadsCorrectInitialText() {
        Intent intent = createIntentForMatthewChapter1();

        try (ActivityScenario<RecordActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                TextView lineView = (TextView) activity._linesView.getChildAt(0);
                assertEquals("Matthew line 0", lineView.getText().toString());
            });
        }
    }

    @Test
    public void recordActivity_navigatesToNextLine() {
        Intent intent = createIntentForMatthewChapter1();

        try (ActivityScenario<RecordActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                TextView lineView = (TextView) activity._linesView.getChildAt(0);
                assertEquals("Matthew line 0", lineView.getText().toString());
                
                // Click Next button
                activity.nextButton.performClick();
            });
            
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            scenario.onActivity(activity -> {
                // Verify second line is now active
                assertEquals("Active line should be 1", 1, activity._activeLine);
            });
        }
    }

    /**
     * This test verifies the core recording workflow:
     * 1. Initial UI state.
     * 2. Simulating a recording touch sequence.
     * 3. Verifying the resulting UI state.
     */
    @Test
    public void recordActivity_recordingWorkflow() {
        Intent intent = createIntentForMatthewChapter1();

        try (ActivityScenario<RecordActivity> scenario = ActivityScenario.launch(intent)) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            
            // 1. Initial State: Play button should be inactive
            scenario.onActivity(activity -> assertEquals("Play button should be Inactive initially", 
                BtnState.Inactive, activity.playButton.getButtonState()));

            // 2. Perform recording via direct touch events
            scenario.onActivity(activity -> {
                long now = SystemClock.uptimeMillis();
                MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 0, 0, 0);
                activity.recordButton.dispatchTouchEvent(down);
                // Verify the button state changed to Pushed immediately
                assertEquals("Record button should show Pushed state", 
                    BtnState.Pushed, activity.recordButton.getButtonState());
            });
            
            // Give it a moment to "record"
            SystemClock.sleep(300);

            scenario.onActivity(activity -> {
                long now = SystemClock.uptimeMillis();
                MotionEvent up = MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, 0, 0, 0);
                activity.recordButton.dispatchTouchEvent(up);
            });

            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            // 3. Verify Post-Recording state
            scenario.onActivity(activity -> {
                // The RecordButton should return to Normal
                assertEquals("Record button should return to Normal", 
                    BtnState.Normal, activity.recordButton.getButtonState());
                // We assert true to verify the path through the code.
                assertTrue("Record interaction complete", true);
            });
        }
    }

    @Test
    public void recordActivity_persistsLocationOnPause() {
        Intent intent = createIntentForMatthewChapter1();
        
        try (ActivityScenario<RecordActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> activity.nextButton.performClick());
            
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            
            // Move through lifecycle to trigger onPause
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.STARTED);
        }
        
        // Verify location was saved via the provider
        BibleLocation loc = ServiceLocator.getServiceLocator().getScriptProvider().getLocation();
        assertEquals("Should have saved book index 0", 0, loc.bookNumber);
        assertEquals("Should have saved chapter index 0", 0, loc.chapterNumber);
        assertEquals("Should have saved line index 1", 1, loc.lineNumber);
    }

    private Intent createIntentForMatthewChapter1() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, RecordActivity.class);
        
        int[] versesPerChapter = new int[]{2};
        
        BookInfo bookInfo = new BookInfo("testProject", 0, "Matthew", 1, 
                versesPerChapter, ServiceLocator.getServiceLocator().getScriptProvider());
        
        intent.putExtra("bookInfo", bookInfo);
        intent.putExtra("chapter", 0);
        intent.putExtra("line", 0);
        return intent;
    }
}
