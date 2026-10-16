package org.sil.hearthis;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/**
 * Unit tests for the lenient number parsing used by FloatPreference.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class FloatPreferenceTest {

    @Test
    public void parseLenient_dotDecimal() {
        assertEquals(1.5f, FloatPreference.parseLenient("1.5"), 0f);
    }

    @Test
    public void parseLenient_commaDecimal() {
        assertEquals(1.5f, FloatPreference.parseLenient("1,5"), 0f);
    }

    @Test
    public void parseLenient_trimsWhitespace() {
        assertEquals(2.0f, FloatPreference.parseLenient(" 2,0 "), 0f);
    }

    @Test
    public void parseLenient_wholeNumber() {
        assertEquals(3f, FloatPreference.parseLenient("3"), 0f);
    }

    @Test(expected = NumberFormatException.class)
    public void parseLenient_invalidThrows() {
        FloatPreference.parseLenient("abc");
    }

    @Test(expected = NumberFormatException.class)
    public void parseLenient_nanThrows() {
        FloatPreference.parseLenient("NaN");
    }

    @Test(expected = NumberFormatException.class)
    public void parseLenient_infinityThrows() {
        FloatPreference.parseLenient("Infinity");
    }

    @Test
    public void clamp_belowMinimum() {
        assertEquals(0.5f, FloatPreference.clamp(0f), 0f);
        assertEquals(0.5f, FloatPreference.clamp(-3f), 0f);
    }

    @Test
    public void clamp_aboveMaximum() {
        assertEquals(5.0f, FloatPreference.clamp(100f), 0f);
    }

    @Test
    public void clamp_inRangeUnchanged() {
        assertEquals(1.5f, FloatPreference.clamp(1.5f), 0f);
        assertEquals(0.5f, FloatPreference.clamp(0.5f), 0f);
        assertEquals(5.0f, FloatPreference.clamp(5.0f), 0f);
    }

    @Test
    public void persistString_clampsAndStoresOutOfRangeValue() {
        Context context = RuntimeEnvironment.getApplication();
        FloatPreference pref = new FloatPreference(context, null);
        pref.setKey("text_scale");
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();
        PreferenceScreen screen = new PreferenceManager(context).createPreferenceScreen(context);
        screen.addPreference(pref);

        pref.setText("50");
        assertEquals(5.0f,
                PreferenceManager.getDefaultSharedPreferences(context).getFloat("text_scale", 1f), 0f);
        assertEquals("5.0", pref.getText());

        pref.setText("0,1");
        assertEquals(0.5f,
                PreferenceManager.getDefaultSharedPreferences(context).getFloat("text_scale", 1f), 0f);
    }

    @Test(expected = NumberFormatException.class)
    public void parseLenient_thousandsSeparatorThrows() {
        FloatPreference.parseLenient("1,234.5");
    }
}
