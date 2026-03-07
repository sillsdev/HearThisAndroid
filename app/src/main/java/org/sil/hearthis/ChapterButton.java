package org.sil.hearthis;

import android.content.Context;
import android.util.AttributeSet;

import script.IScriptProvider;

/**
 * Button for selecting a chapter in a book (used in ChooseChapterActivity)
 */
public class ChapterButton extends ProgressButton {
    public ChapterButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    IScriptProvider scriptProvider;
    int bookNumber;
    int chapterNumber;

    public void init(IScriptProvider provider, int bookNumber, int chapterNumber)
    {
        scriptProvider = provider;
        this.bookNumber = bookNumber;
        this.chapterNumber = chapterNumber;
    }

    @Override
    protected boolean isAllRecorded() {
        // Added null check for scriptProvider to avoid NullPointerException during Android Studio Preview
        if (scriptProvider == null) {
            return false;
        }
        int transLines = scriptProvider.GetTranslatedLineCount(bookNumber, chapterNumber);
        int actualLines = scriptProvider.GetScriptLineCount(bookNumber, chapterNumber);
        return actualLines > 0 && transLines == actualLines;
    }

    @Override
    protected String getLabel() {
        return Integer.toString(chapterNumber);
    }

    @Override
    protected int getForeColor() {
        // Added null check for scriptProvider to avoid NullPointerException during Android Studio Preview
        if (scriptProvider == null || scriptProvider.GetTranslatedLineCount(bookNumber, chapterNumber) == 0)
            return R.color.navButtonUntranslatedColor;
        return super.getForeColor();
    }
}
