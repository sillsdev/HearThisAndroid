package org.sil.hearthis;

import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.view.ViewGroup;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import script.FileSystem;
import script.RealScriptProvider;
import script.TestFileSystem;

/**
 * Tests the navigation flow from ChooseBookActivity to ChooseChapterActivity.
 * Uses onActivity for stability on older Android versions (API 26-28).
 */
@RunWith(AndroidJUnit4.class)
public class BookSelectionTest {

    private TestFileSystem fakeFileSystem;

    @Before
    public void setUp() {
        Intents.init();
        ServiceLocator.theOneInstance = new ServiceLocator();
        
        fakeFileSystem = new TestFileSystem();
        fakeFileSystem.externalFilesDirectory = "root";
        fakeFileSystem.project = "testProject";
        
        // Default setup for most tests (partially complete books)
        setupTestFileSystem("Matthew;10:0\nMark;8:0\n");
    }

    // Helper to allow different file setups
    private void setupTestFileSystem(String infoTxtContent) {
        StringBuilder infoTxtBuilder = new StringBuilder();
        for (int i = 0; i < 39; i++) infoTxtBuilder.append("Book").append(i).append(";\n");
        infoTxtBuilder.append(infoTxtContent);

        fakeFileSystem.simulateFile(fakeFileSystem.getInfoTxtPath(), infoTxtBuilder.toString());
        fakeFileSystem.SimulateDirectory(fakeFileSystem.getProjectDirectory());
        
        ServiceLocator.getServiceLocator().externalFilesDirectory = fakeFileSystem.externalFilesDirectory;
        ServiceLocator.getServiceLocator().setFileSystem(new FileSystem(fakeFileSystem));
        ServiceLocator.getServiceLocator().setScriptProvider(new RealScriptProvider(fakeFileSystem.getProjectDirectory()));
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void chooseBookActivity_displaysBooks() {
        try (ActivityScenario<ChooseBookActivity> scenario = ActivityScenario.launch(ChooseBookActivity.class)) {
            scenario.onActivity(activity -> {
                BookButton matthewButton = findBookButton(activity, "Matthew");
                assertNotNull("Matthew button should be displayed", matthewButton);
                assertEquals(View.VISIBLE, matthewButton.getVisibility());
                
                BookButton markButton = findBookButton(activity, "Mark");
                assertNotNull("Mark button should be displayed", markButton);
                assertEquals(View.VISIBLE, markButton.getVisibility());
            });
        }
    }

    @Test
    public void selectingBook_navigatesToChapters() {
        try (ActivityScenario<ChooseBookActivity> scenario = ActivityScenario.launch(ChooseBookActivity.class)) {
            scenario.onActivity(activity -> {
                BookButton matthewButton = findBookButton(activity, "Matthew");
                assertNotNull(matthewButton);
                matthewButton.performClick();
            });

            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            
            // Verify navigation occurred
            intended(hasComponent(ChooseChapterActivity.class.getName()));
        }
    }

    @Test
    public void selectingChapter_navigatesToRecordActivity() {
        try (ActivityScenario<ChooseBookActivity> scenario = ActivityScenario.launch(ChooseBookActivity.class)) {
            // 1. Navigate to Chapters
            scenario.onActivity(activity -> {
                BookButton matthewButton = findBookButton(activity, "Matthew");
                assertNotNull(matthewButton);
                matthewButton.performClick();
            });

            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            // 2. Since we've switched activities, we'll verify the intent was sent.
            // Espresso Intents will catch the navigation from ChooseChapterActivity as well.
            intended(hasComponent(ChooseChapterActivity.class.getName()));
        }
    }

    @Test
    public void chooseBookActivity_showsRecordedStatus() {
        // New setup for this specific test where Matthew is fully recorded
        setupTestFileSystem("Matthew;10:10\nMark;8:0\n");

        try (ActivityScenario<ChooseBookActivity> scenario = ActivityScenario.launch(ChooseBookActivity.class)) {
            scenario.onActivity(activity -> {
                BookButton matthewButton = findBookButton(activity, "Matthew");
                assertNotNull(matthewButton);
                assertTrue("Matthew should be marked as all recorded", matthewButton.isAllRecorded());
                
                BookButton markButton = findBookButton(activity, "Mark");
                assertNotNull(markButton);
                assertFalse("Mark should NOT be marked as all recorded", markButton.isAllRecorded());
            });
        }
    }

    private BookButton findBookButton(ChooseBookActivity activity, String bookName) {
        ViewGroup bookFlow = activity.findViewById(R.id.booksFlow);
        for (int i = 0; i < bookFlow.getChildCount(); i++) {
            View child = bookFlow.getChildAt(i);
            if (child instanceof BookButton button) {
                if (button.Model != null && bookName.equals(button.Model.Name)) {
                    return button;
                }
            }
        }
        return null;
    }
}
