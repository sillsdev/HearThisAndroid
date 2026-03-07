package org.sil.hearthis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import script.BookInfo;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class BookButtonTest {

    private Context context;
    private TestScriptProvider scriptProvider;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        scriptProvider = new TestScriptProvider();
    }

    private BookInfo createBookInfo(int bookNumber, String abbr) {
        BookInfo info = new BookInfo("test", bookNumber, "Genesis", 50, new int[50], scriptProvider);
        info.Abbr = abbr;
        return info;
    }

    @Test
    public void testGetForeColor_NothingTranslated_ReturnsGrey() {
        scriptProvider.setTranslatedBookCount(0, 0);

        BookButton button = new BookButton(context, null);
        button.Model = createBookInfo(0, "gen");

        assertEquals(R.color.navButtonUntranslatedColor, button.getForeColor());
    }

    @Test
    public void testGetForeColor_SomethingTranslated_Joshua_ReturnsHistoryColor() {
        scriptProvider.setTranslatedBookCount(6, 3);

        BookButton button = new BookButton(context, null);
        button.Model = createBookInfo(6, "josh");

        assertEquals(R.color.navButtonHistoryColor, button.getForeColor());
    }

    @Test
    public void testGetForeColor_SomethingTranslated_Genesis_ReturnsLawColor() {
        scriptProvider.setTranslatedBookCount(0, 3);

        BookButton button = new BookButton(context, null);
        button.Model = createBookInfo(0, "gen");

        assertEquals(R.color.navButtonLawColor, button.getForeColor());
    }

    @Test
    public void testGetLabel_NumericAbbr_FormatsCorrectly() {
        BookButton button = new BookButton(context, null);
        button.Model = createBookInfo(18, "1sam");

        assertEquals("1Sam", button.getLabel());
    }

    @Test
    public void testGetLabel_AlphaAbbr_FormatsCorrectly() {
        BookButton button = new BookButton(context, null);
        button.Model = createBookInfo(0, "gen");

        assertEquals("Gen", button.getLabel());
    }

    @Test
    public void testIsAllRecorded_Partial_ReturnsFalse() {
        scriptProvider.setTranslatedBookCount(0, 5);
        scriptProvider.setScriptLineCount(0, 10);

        BookButton button = new BookButton(context, null);
        button.Model = createBookInfo(0, "gen");

        assertFalse(button.isAllRecorded());
    }

    @Test
    public void testIsAllRecorded_Complete_ReturnsTrue() {
        scriptProvider.setTranslatedBookCount(0, 10);
        scriptProvider.setScriptLineCount(0, 10);

        BookButton button = new BookButton(context, null);
        button.Model = createBookInfo(0, "gen");

        assertTrue(button.isAllRecorded());
    }
}
