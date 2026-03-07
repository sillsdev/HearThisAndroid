package org.sil.hearthis;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/**
 * Unit tests for preference persistence.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class HearThisPreferencesTest {

    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        // Clear preferences before each test
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().clear().commit();
    }

    @Test
    public void preferences_saveAndRetrieveTextScale() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        
        // Verify default
        float defaultScale = prefs.getFloat("text_scale", 1.0f);
        assertEquals(1.0f, defaultScale, 0.001f);

        // Save a new value
        prefs.edit().putFloat("text_scale", 1.5f).commit();

        // Retrieve and verify
        float newScale = prefs.getFloat("text_scale", 1.0f);
        assertEquals(1.5f, newScale, 0.001f);
    }

    @Test
    public void preferences_handlesMissingKeysWithDefaults() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        
        float scale = prefs.getFloat("non_existent_key", 2.0f);
        assertEquals(2.0f, scale, 0.001f);
    }
}
