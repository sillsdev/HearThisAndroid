package org.sil.hearthis;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/**
 * Unit tests for LevelMeterView logic.
 * Note: We bypass resource loading by using a mock context or assuming values
 * if the environment can't provide the R.color values.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class LevelMeterViewTest {

    private LevelMeterView levelMeterView;

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.getApplication();
        // levelMeterView.init(context) will try to load colors.
        // If it fails, we wrap it to ensure logic tests can still run.
        try {
            levelMeterView = new LevelMeterView(context, null);
        } catch (Exception e) {
            // Fallback for logic-only testing if resources aren't bound
            levelMeterView = new LevelMeterView(context, null) {
                @Override
                void init(Context c) {
                    // No-op to avoid resource loading in this environment
                }
            };
        }
    }

    @Test
    public void setLevel_updatesDisplayLevelAfterThrottle() throws InterruptedException {
        // Initial state
        assertEquals(0, levelMeterView.displayLevel);

        // First update should happen after the 100ms threshold
        levelMeterView.setLevel(50);
        
        // We need to wait > 100ms because of the throttle logic in LevelMeterView
        Thread.sleep(150);
        
        levelMeterView.setLevel(75);
        assertEquals(75, levelMeterView.displayLevel);
    }

    @Test
    public void setLevel_capturesMaxLevelDuringThrottle() throws InterruptedException {
        levelMeterView.setLevel(10);
        
        // Rapid updates within the 100ms window
        levelMeterView.setLevel(80);
        levelMeterView.setLevel(40);
        levelMeterView.setLevel(90);
        
        // Wait for throttle window to expire
        Thread.sleep(150);
        levelMeterView.setLevel(20); // This trigger should pick up the max (90)
        
        assertEquals("Should display the peak level seen during the interval", 90, levelMeterView.displayLevel);
    }
}
