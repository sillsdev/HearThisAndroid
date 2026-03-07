package org.sil.hearthis;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;

import androidx.core.content.ContextCompat;

/**
 * Created by Thomson on 3/6/2016.
 */
public class PlayButton extends CustomButton {
    public PlayButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        blueFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        blueFillPaint.setColor(ContextCompat.getColor(context, R.color.audioButtonBlueColor));
        highlightBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightBorderPaint.setColor(ContextCompat.getColor(context, R.color.buttonSuggestedBorderColor));
        highlightBorderPaint.setStrokeWidth(4f);
        highlightBorderPaint.setStyle(Paint.Style.STROKE);

        disabledPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        disabledPaint.setColor(ContextCompat.getColor(context, R.color.audioButtonDisabledColor));

        playBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        playBorderPaint.setColor(ContextCompat.getColor(context, R.color.buttonSuggestedBorderColor));
        playBorderPaint.setStrokeWidth(6f);
        playBorderPaint.setStyle(Paint.Style.STROKE);
    }
    final Paint blueFillPaint;
    final Paint highlightBorderPaint;
    final Paint disabledPaint;
    final Paint playBorderPaint;

    boolean playing;
    boolean getPlaying() { return playing;}
    void setPlaying(boolean val) {playing = val; }
    private final Path arrow = new Path();

    @Override
    public void onDraw(Canvas canvas) {
        //super.onDraw(canvas);
        arrow.reset();
        int w = getWidth();
        int h = getHeight();
        float moveWhenPushed = 1.0f;
        float inset = 1; // a margin to prevent clipping the shape
        float size = Math.min(w, h) - moveWhenPushed - inset;
        float deltaX = (w - size) / 2f + (getButtonState() == BtnState.Pushed || getPlaying() ? moveWhenPushed : 0f);
        float deltaY = (h - size) / 2f + (getButtonState() == BtnState.Pushed || getPlaying() ? moveWhenPushed : 0f);

        arrow.moveTo(deltaX, deltaY);
        arrow.lineTo(deltaX, size + deltaY);
        arrow.lineTo(size + deltaX, size / 2 + deltaY);
        arrow.lineTo(deltaX, deltaY);
        if (getPlaying()) {
            canvas.drawPath(arrow, blueFillPaint);
            canvas.drawPath(arrow, playBorderPaint);
        }
        switch (getButtonState())
        {
            case Normal:
                canvas.drawPath(arrow, blueFillPaint);
                if (getIsDefault())
                    canvas.drawPath(arrow, highlightBorderPaint);
                break;
            case Pushed:
                canvas.drawPath(arrow, blueFillPaint);
                break;
            case Inactive:
                canvas.drawPath(arrow, disabledPaint);
                break;
        }
    }
}
