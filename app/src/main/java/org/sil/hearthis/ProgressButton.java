package org.sil.hearthis;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

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
    private final Rect _rect = new Rect(); // Pre-allocate to avoid GC jank

    public ProgressButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    void init() {
        _forePaint = new Paint();
        _forePaint.setColor(ContextCompat.getColor(getContext(), getForeColor()));
        _textPaint = new Paint();
        _textPaint.setColor(ContextCompat.getColor(getContext(), R.color.navButtonTextColor));
        _textPaint.setTextAlign(Paint.Align.CENTER);
        int fontSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                16, getResources().getDisplayMetrics());
        _textPaint.setTextSize(fontSize);
        _highlitePaint = new Paint();
        _highlitePaint.setColor(ContextCompat.getColor(getContext(), R.color.navButtonHiliteColor));
    }

    // Intended to be overidden by bookButton, which uses different colors for different
    // groups of books.
    protected int getForeColor() {
        return R.color.navButtonColor;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Try for a width based on our minimum
        int minW = getPaddingLeft() + getPaddingRight() + getSuggestedMinimumWidth() * 5/4;
        int w = (int) (minW + getExtraWidth());
        int h = getPaddingBottom() + getPaddingTop() + getSuggestedMinimumHeight() * 3 / 2;

        setMeasuredDimension(w, h);
    }

    // BookButton overrides to stretch longer books for easier recognition
    protected double getExtraWidth() {
        return 0;
    }

    @Override
    public void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (_forePaint == null) {
            init();
        }
        
        int width = getWidth();
        int height = getHeight();
        
        // Update the existing rect instead of creating a new one
        _rect.set(2, 3, width - 2, height - 2);
        canvas.drawRect(_rect, _forePaint);
        
        // Logic would suggest vertical position of height/2, but centering
        // seems to align baseline, so that works out a bit high. 3/5 seems to produce
        // something that actually looks centered.
        canvas.drawText(getLabel(), width / 2f, height * 3 / 5f, _textPaint);

        if (isAllRecorded()) {
            int mid = height / 2;
            int leftTick = mid / 5;
            int halfWidth = mid / 3;
            int v1 = mid + halfWidth * 2 / 3;
            int v2 = mid + halfWidth * 5 / 3;
            int v3 = mid - halfWidth * 4 / 3;

            //draw the first stroke of a check mark
            _highlitePaint.setStrokeWidth((float)4.0);
            canvas.drawLine(leftTick, v1, (float)leftTick + halfWidth, v2, _highlitePaint);
            //complete the checkmark
            canvas.drawLine((float)leftTick + halfWidth, v2, (float)leftTick + halfWidth * 2, v3, _highlitePaint);
        }
    }

    protected abstract String getLabel(); // text to show in button
    protected abstract boolean isAllRecorded(); // everything button represents is recorded already
}
