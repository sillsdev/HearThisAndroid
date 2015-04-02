package org.sil.hearthis;

import Script.BookInfo;

/**
 * This class supports creating a BookInfo in a suitable default state for testing.
 * Something like
 * new BookInfoProvider().setBookNumber(1).info().
 * Methods may be chained (and new ones defined as needed) to initialize whatever
 * you like.
 * By default it returns Genesis from the 'test' project, which has nothing translated but 50 chapters.
 * currently versesPerChapter defaults to an array decreasing from N to 1
 */
public class BookInfoProvider {
    int bookNumber;
    TestScriptProvider provider;
    // Enhance: the plan is that eventually each argument will be taken from a
    // member variable, which is initialized this way but can be overridden using
    // methods like setBookNumber() (not written yet).
    public BookInfo info() {
        return new BookInfo("test", bookNumber, "Genesis", 50,
            makeVersesPerChapter(50), getScriptProvider());
    }

    // Gives some sort of default, where the numbers are neither all
    // the same nor identical to the index nor unpredictable, without
    // the cost of getting true data.
    public int[] makeVersesPerChapter(int chapters)
    {
        int[] versesPerChapter = new int[chapters];
        for (int i = 0; i < chapters; i++)
            versesPerChapter[i] = chapters - i;
        return versesPerChapter;
    }

    public BookInfoProvider setBookNumber(int bn) {
        bookNumber = bn;
        return this;
    }

    public TestScriptProvider getScriptProvider() {
        if (provider == null)
            provider = new TestScriptProvider();
        return provider;
    }

    // Call this after any change to bookNumber!
    public BookInfoProvider setTranslatedVersesPerBook(int verses) {
        getScriptProvider().setTranslatedBookCount(bookNumber, verses);
        return this;
    }
}
