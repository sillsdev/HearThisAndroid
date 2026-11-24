package org.sil.hearthis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.os.Build;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumentation tests for SyncActivity.
 * Focuses on UI state, CameraX initialization, and SyncService integration.
 * Uses onActivity assertions for stability on older Android versions (API 26-28).
 */
@RunWith(AndroidJUnit4.class)
public class SyncActivityTest {

    @Rule
    public GrantPermissionRule permissionRule = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
            GrantPermissionRule.grant(Manifest.permission.CAMERA, Manifest.permission.POST_NOTIFICATIONS) :
            GrantPermissionRule.grant(Manifest.permission.CAMERA);

    @Before
    public void setUp() {
        ServiceLocator.theOneInstance = new ServiceLocator();
    }

    @Test
    public void syncActivity_initialState_showsIpAddress() {
        try (ActivityScenario<SyncActivity> scenario = ActivityScenario.launch(SyncActivity.class)) {
            scenario.onActivity(activity -> {
                assertEquals("Progress view should be visible", View.VISIBLE, activity.progressView.getVisibility());
                assertEquals("Continue button should be visible", View.VISIBLE, activity.continueButton.getVisibility());
                assertFalse("Continue button should be disabled initially", activity.continueButton.isEnabled());
                
                View ourIpView = activity.findViewById(R.id.our_ip_address);
                assertEquals("Our IP address view should be visible", View.VISIBLE, ourIpView.getVisibility());
            });
        }
    }

    @Test
    public void syncActivity_startsCamera_onScanClick() {
        try (ActivityScenario<SyncActivity> scenario = ActivityScenario.launch(SyncActivity.class)) {
            // Trigger scan button click on the UI thread
            scenario.onActivity(activity -> activity.scanBtn.performClick());
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            scenario.onActivity(activity -> {
                assertEquals("PreviewView should become visible", View.VISIBLE, activity.previewView.getVisibility());
                assertTrue("Activity should be in scanning state", activity.scanning);
            });
        }
    }

    @Test
    public void syncActivity_serviceIntegration_updatesStatus() {
        try (ActivityScenario<SyncActivity> scenario = ActivityScenario.launch(SyncActivity.class)) {
            // Simulate a notification from the sync server
            scenario.onActivity(activity -> activity.onNotification("Connected to Desktop"));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            scenario.onActivity(activity -> {
                String expectedText = activity.getString(R.string.sync_success);
                assertEquals("Progress view should show success message", expectedText, activity.progressView.getText().toString());
                assertTrue("Continue button should be enabled", activity.continueButton.isEnabled());
            });
        }
    }

    @Test
    public void syncActivity_fileTransfer_updatesProgress() {
        try (ActivityScenario<SyncActivity> scenario = ActivityScenario.launch(SyncActivity.class)) {
            final String testPath = "Genesis/1/1.wav";
            
            // Simulate receiving a file
            scenario.onActivity(activity -> activity.receivingFile(testPath));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            scenario.onActivity(activity -> {
                String expectedText = activity.getString(R.string.receiving_file, testPath);
                assertEquals("Progress view should show receiving file path", expectedText, activity.progressView.getText().toString());
            });
        }
    }

    @Test
    public void syncActivity_clickContinue_finishesActivity() {
        try (ActivityScenario<SyncActivity> scenario = ActivityScenario.launch(SyncActivity.class)) {
            // Enable and click Continue
            scenario.onActivity(activity -> {
                activity.onNotification("Success");
                activity.continueButton.performClick();
            });
            
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            
            // Verify activity is finishing or already destroyed
            try {
                scenario.onActivity(activity -> assertTrue("Activity should be finishing", activity.isFinishing()));
            } catch (NullPointerException e) {
                // Already destroyed is also fine
                if (e.getMessage() != null && !e.getMessage().contains("destroyed already")) {
                    throw e;
                }
            }
        }
    }
}
