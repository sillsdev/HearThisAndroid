package org.sil.hearthis;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Thomson on 3/27/2015.
 * This is a base class for BookButton and ChapterButton, buttons that indicate
 * with a check mark when a book or chapter is fully translated, and with a paler
 * background when there is nothing available to translate
 */
public abstract class ProgressButton extends View {

    protected Paint _forePaint;
    protected Paint _textPaint;
    protected Paint _highlitePaint;

    public ProgressButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    void init() {
        _forePaint = new Paint();
        _forePaint.setColor(getResources().getColor(getForeColor()));
        _textPaint = new Paint();
        _textPaint.setColor(getResources().getColor(R.color.navButtonTextColor));
        _textPaint.setTextAlign(Paint.Align.CENTER);
        _highlitePaint = new Paint();
        _highlitePaint.setColor(getResources().getColor(R.color.navButtonHiliteColor));
    }

    // Intended to be overidden by bookButton, which uses different colors for different
    // groups of books.
    protected int getForeColor() {
        return R.color.navButtonColor;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Try for a width based on our minimum
        int minw = getPaddingLeft() + getPaddingRight() + getSuggestedMinimumWidth() * 5/4;
        int w = (int) (minw + getExtraWidth());
        int h = getPaddingBottom() + getPaddingTop() + getSuggestedMinimumHeight() * 3 / 2;

        setMeasuredDimension(w, h);
    }

    // BookButton overrides to stretch longer books for easier recognition
    protected double getExtraWidth() {
        return 0;
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
        canvas.drawText(getLabel(), (right - left)/2, (bottom - top)/2, _textPaint);

        if (isAllRecorded()) {
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

    protected abstract String getLabel(); // text to show in button
    protected abstract boolean isAllRecorded(); // everything button represents is recorded already
}
