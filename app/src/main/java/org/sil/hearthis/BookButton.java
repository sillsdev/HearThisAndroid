package org.sil.hearthis;

import Script.BookInfo;
import Script.IScriptProvider;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;


public class BookButton  extends View {

	public BookInfo Model;
	Paint _forePaint;
	Paint _textPaint;
	Paint _highlitePaint;
	public BookButton(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}
	
	public BookButton(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	void init() {
		_forePaint = new Paint();
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
		_forePaint.setColor(getResources().getColor(navbuttoncolor));
		_textPaint = new Paint();
		_textPaint.setColor(getResources().getColor(R.color.navButtonTextColor));
		_textPaint.setTextAlign(Paint.Align.CENTER);
        _highlitePaint = new Paint();
        _highlitePaint.setColor(getResources().getColor(R.color.navButtonHiliteColor));
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
	   // Try for a width based on our minimum
	   int minw = getPaddingLeft() + getPaddingRight() + getSuggestedMinimumWidth() * 5/4;
	   int kMaxChapters = 150;//psalms
	   int w = (int) (minw + ((double)Model.ChapterCount / (double)kMaxChapters) * 150.0);
	   int h = getPaddingBottom() + getPaddingTop() + getSuggestedMinimumHeight() * 3 / 2;

	   setMeasuredDimension(w, h);
	}
	
	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (_forePaint == null) {
			init();
		}
	    int right = this.getRight();
	    int left = this.getLeft();
	    int bottom = this.getBottom();
	    int top = this.getTop();
        Rect r = new Rect(2, 3, right - left - 2, bottom - top - 2);
        canvas.drawRect(r, _forePaint);
        char first = Model.Abbr.charAt(0);
        String abbr = Model.Abbr;
        if (first >= '0' && first <= '9') {
        	abbr = abbr.substring(0,1) + abbr.substring(1,2).toUpperCase() + abbr.substring(2);
        }
        else {
        	abbr = abbr.substring(0,1).toUpperCase() + abbr.substring(1);
        }
		canvas.drawText(abbr, (right - left)/2, (bottom - top)/2, _textPaint);

        BookInfo book = (BookInfo)this.Model;
        IScriptProvider provider = book.getScriptProvider();
        int transLines = provider.GetTranslatedLineCount(book.BookNumber);
        int actualLines = provider.GetScriptLineCount(book.BookNumber);
        if (actualLines > 0 && transLines == actualLines) {
            int mid = (bottom - top) / 2;
            int leftTick = mid / 5;
            int halfWidth = mid / 3;
            int v1 = mid + halfWidth * 2 / 3;
            int v2 = mid + halfWidth * 5 / 3;
            int v3 = mid - halfWidth * 4 / 3;

            //draw the first stroke of a check mark
            _highlitePaint.setStrokeWidth((float)4.0);
            canvas.drawLine(leftTick, v1, leftTick+halfWidth, v2, _highlitePaint);
            //complete the checkmark
            canvas.drawLine(leftTick+halfWidth, v2, leftTick + halfWidth * 2, v3, _highlitePaint);
        }
	}

}
