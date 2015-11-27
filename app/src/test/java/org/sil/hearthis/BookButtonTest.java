package org.sil.hearthis;

import junit.framework.TestCase;

import org.junit.Assert;

import Script.BookInfo;

import static org.junit.Assert.*;

public class BookButtonTest {
    public BookButtonTest()
    {}

    @org.junit.Test
    public void testGetForeColor_NothingTranslated_ReturnsGrey() throws Exception {
        BookButton button = new BookButton(null, null);
        button.Model = new BookInfoProvider().info();
        Assert.assertEquals(R.color.navButtonUntranslatedColor, button.getForeColor());
    }

    @org.junit.Test
    public void testGetForeColor_SomethingTranslated_Joshua_ReturnsHistroyColor() throws Exception {
        BookButton button = new BookButton(null, null);
        button.Model = new BookInfoProvider().setBookNumber(6).setTranslatedVersesPerBook(3).info();
        Assert.assertEquals(R.color.navButtonHistoryColor, button.getForeColor());
    }

    @org.junit.Test
    public void testGetForeColor_SomethingTranslated_Genesis_ReturnsLawColor() throws Exception {
        BookButton button = new BookButton(null, null);
        button.Model = new BookInfoProvider().setTranslatedVersesPerBook(3).info();
        Assert.assertEquals(R.color.navButtonLawColor, button.getForeColor());
    }

    @org.junit.Test
    public void testGetExtraWidth() throws Exception {

    }

    @org.junit.Test
    public void testIsAllRecorded() throws Exception {

    }

    @org.junit.Test
    public void testGetLabel() throws Exception {

    }
}