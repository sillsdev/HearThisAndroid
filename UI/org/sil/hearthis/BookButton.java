package org.sil.hearthis;

import org.sil.hearthis.R;
import Script.BookInfo;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;


public class BookButton  extends View {

	public BookInfo Model;
	Paint _forePaint;
	Paint _highlitePaint;
	public BookButton(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}
	
	public BookButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	void init() {
		_forePaint = new Paint();
		_forePaint.setColor(getResources().getColor(R.color.navButtonColor));		
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
	   // Try for a width based on our minimum
	   int minw = getPaddingLeft() + getPaddingRight() + getSuggestedMinimumWidth();
	   int kMaxChapters = 150;//psalms
	   int w = (int) (minw + ((double)Model.ChapterCount / (double)kMaxChapters) * 33.0);
	   int h = getPaddingBottom() + getPaddingTop() + getSuggestedMinimumHeight();

	   setMeasuredDimension(w, h);
	}
	
	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
	    int right = this.getRight();
	    int left = this.getLeft();
	    int bottom = this.getBottom();
	    int top = this.getTop();
        Rect r = new Rect(2, 3, right - left - 2, bottom - top - 2);
        canvas.drawRect(r, _forePaint);
	}

}
