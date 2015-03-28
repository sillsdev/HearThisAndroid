package org.sil.hearthis;

import Script.BookInfo;
import Script.IScriptProvider;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;


public class BookButton  extends ProgressButton {

	public BookInfo Model;

	public BookButton(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

    @Override
    protected int getForeColor() {
        int navbuttoncolor = R.color.navButtonColor;
        if (Model.BookNumber < 5) {
            navbuttoncolor = R.color.navButtonLawColor;
        }
        else if (Model.BookNumber < 17) {
            navbuttoncolor = R.color.navButtonHistoryColor;
        }
        else if (Model.BookNumber < 22) {
            navbuttoncolor = R.color.navButtonPoetryColor;
        }
        else if (Model.BookNumber < 27) {
            navbuttoncolor = R.color.navButtonMajorProphetColor;
        }
        else if (Model.BookNumber < 39) {
            navbuttoncolor = R.color.navButtonMinorProphetColor;
        }
        else if (Model.BookNumber < 43) {
            navbuttoncolor = R.color.navButtonGospelsColor;
        }
        else if (Model.BookNumber < 44) {
            navbuttoncolor = R.color.navButtonActsColor;
        }
        else if (Model.BookNumber < 57) {
            navbuttoncolor = R.color.navButtonPaulineColor;
        }
        else if (Model.BookNumber < 65) {
            navbuttoncolor = R.color.navButtonEpistlesColor;
        }
        else {
            navbuttoncolor = R.color.navButtonRevelationColor;
        }
        return navbuttoncolor;
    }

    @Override
    protected double getExtraWidth() {
        int kMaxChapters = 150;//psalms
        return ((double)Model.ChapterCount / kMaxChapters) * 150.0;
    }

    @Override
    protected boolean isAllRecorded() {
        BookInfo book = this.Model;
        IScriptProvider provider = book.getScriptProvider();
        int transLines = provider.GetTranslatedLineCount(book.BookNumber);
        int actualLines = provider.GetScriptLineCount(book.BookNumber);
        return actualLines > 0 && transLines == actualLines;
    }

    @Override
    protected String getLabel() {
        char first = Model.Abbr.charAt(0);
        String abbr = Model.Abbr;
        if (first >= '0' && first <= '9') {
        	abbr = abbr.substring(0,1) + abbr.substring(1,2).toUpperCase() + abbr.substring(2);
        }
        else {
        	abbr = abbr.substring(0,1).toUpperCase() + abbr.substring(1);
        }
        return abbr;
    }

}
