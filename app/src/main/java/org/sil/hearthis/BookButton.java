package org.sil.hearthis;

import script.BookInfo;
import script.IScriptProvider;

import android.content.Context;
import android.util.AttributeSet;

public class BookButton  extends ProgressButton {

	public BookInfo Model;

	public BookButton(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

    @Override
    protected int getForeColor() {
        // Added null check for Model to prevent NullPointerException during rendering in IDE.
        if (Model == null) {
            return super.getForeColor();
        }
        if (Model.getScriptProvider().GetTranslatedLineCount(Model.BookNumber) == 0)
            return R.color.navButtonUntranslatedColor;
        if (Model.BookNumber < 5) {
            return R.color.navButtonLawColor;
        }
        else if (Model.BookNumber < 17) {
            return R.color.navButtonHistoryColor;
        }
        else if (Model.BookNumber < 22) {
            return R.color.navButtonPoetryColor;
        }
        else if (Model.BookNumber < 27) {
            return R.color.navButtonMajorProphetColor;
        }
        else if (Model.BookNumber < 39) {
            return R.color.navButtonMinorProphetColor;
        }
        else if (Model.BookNumber < 43) {
            return R.color.navButtonGospelsColor;
        }
        else if (Model.BookNumber < 44) {
            return R.color.navButtonActsColor;
        }
        else if (Model.BookNumber < 57) {
            return R.color.navButtonPaulineColor;
        }
        else if (Model.BookNumber < 65) {
            return R.color.navButtonEpistlesColor;
        }
        else {
            return R.color.navButtonRevelationColor;
        }
    }

    @Override
    protected double getExtraWidth() {
        // Added null check for Model to prevent NullPointerException during rendering in IDE.
        if (Model == null) {
            return 0;
        }
        int kMaxChapters = 150;//psalms
        return ((double)Model.ChapterCount / kMaxChapters) * 150.0;
    }

    @Override
    protected boolean isAllRecorded() {
        // Added null check for Model to prevent NullPointerException during rendering in IDE.
        if (Model == null) {
            return false;
        }
        BookInfo book = this.Model;
        IScriptProvider provider = book.getScriptProvider();
        int transLines = provider.GetTranslatedLineCount(book.BookNumber);
        int actualLines = provider.GetScriptLineCount(book.BookNumber);
        return actualLines > 0 && transLines == actualLines;
    }

    @Override
    protected String getLabel() {
        // Added null check for Model and Abbr to prevent NullPointerException during rendering in IDE.
        if (Model == null || Model.Abbr == null || Model.Abbr.isEmpty()) {
            return "";
        }
        char first = Model.Abbr.charAt(0);
        String abbr = Model.Abbr;
        if (first >= '0' && first <= '9') {
        	abbr = abbr.charAt(0) + abbr.substring(1,2).toUpperCase() + abbr.substring(2);
        }
        else {
        	abbr = abbr.substring(0,1).toUpperCase() + abbr.substring(1);
        }
        return abbr;
    }

}
