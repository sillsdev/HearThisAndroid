package org.sil.hearthis;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import script.FileSystem;
import script.TestFileSystem;

/**
 * Tests the ChooseProjectActivity logic for listing and selecting projects.
 */
@RunWith(AndroidJUnit4.class)
public class ProjectSelectionTest {

    private TestFileSystem fakeFileSystem;

    @Before
    public void setUp() {
        Intents.init();
        ServiceLocator.theOneInstance = new ServiceLocator();
        
        fakeFileSystem = new TestFileSystem();
        fakeFileSystem.externalFilesDirectory = "root";
        ServiceLocator.getServiceLocator().externalFilesDirectory = fakeFileSystem.externalFilesDirectory;
        ServiceLocator.getServiceLocator().setFileSystem(new FileSystem(fakeFileSystem));
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void chooseProjectActivity_listsAvailableProjects() {
        // Setup multiple project directories
        fakeFileSystem.SimulateDirectory("root/ProjectA");
        fakeFileSystem.SimulateDirectory("root/ProjectB");
        
        try (ActivityScenario<ChooseProjectActivity> ignored = ActivityScenario.launch(ChooseProjectActivity.class)) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            
            // Verify both project names appear in the list
            onView(withText("ProjectA")).check(matches(isDisplayed()));
            onView(withText("ProjectB")).check(matches(isDisplayed()));
        }
    }

    @Test
    public void selectingProject_navigatesToChooseBook() {
        // Setup one project with a valid info.txt so it can load books
        fakeFileSystem.SimulateDirectory("root/ProjectA");
        fakeFileSystem.simulateFile("root/ProjectA/info.txt", "Matthew;10:0\n");
        
        try (ActivityScenario<ChooseProjectActivity> ignored = ActivityScenario.launch(ChooseProjectActivity.class)) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            // Click on ProjectA in the list
            onData(allOf(is(instanceOf(String.class)), is("ProjectA"))).perform(click());

            // Verify navigation to ChooseBookActivity (since there's no saved location)
            intended(hasComponent(ChooseBookActivity.class.getName()));
        }
    }

    @Test
    public void selectingProject_withSavedLocation_navigatesToRecord() {
        // Setup a project with a saved location in status.txt
        fakeFileSystem.SimulateDirectory("root/ProjectA");
        fakeFileSystem.simulateFile("root/ProjectA/info.txt", "Matthew;10:0\n");
        // Status: Book 0, Chapter 0, Line 5
        fakeFileSystem.simulateFile("root/ProjectA/status.txt", "0;0;5");
        
        try (ActivityScenario<ChooseProjectActivity> ignored = ActivityScenario.launch(ChooseProjectActivity.class)) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            // Click on ProjectA
            onData(allOf(is(instanceOf(String.class)), is("ProjectA"))).perform(click());

            // Verify navigation directly to RecordActivity
            intended(hasComponent(RecordActivity.class.getName()));
        }
    }
}
