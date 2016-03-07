package org.sil.hearthis;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;

/**
 * Created by Thomson on 3/6/2016.
 */
public class PlayButton extends CustomButton {
    public PlayButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        blueFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        blueFillPaint.setColor(context.getResources().getColor(R.color.audioButtonBlueColor));
        highlightBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightBorderPaint.setColor(context.getResources().getColor(R.color.buttonSuggestedBorderColor));
        highlightBorderPaint.setStrokeWidth(4f);
        highlightBorderPaint.setStyle(Paint.Style.STROKE);

        disabledPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        disabledPaint.setColor(context.getResources().getColor(R.color.audioButtonDisabledColor));

        playBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        playBorderPaint.setColor(context.getResources().getColor(R.color.buttonSuggestedBorderColor));
        playBorderPaint.setStrokeWidth(6f);
        playBorderPaint.setStyle(Paint.Style.STROKE);
    }
    Paint blueFillPaint;
    Paint highlightBorderPaint;
    Paint disabledPaint;
    Paint playBorderPaint;

    boolean playing;
    boolean getPlaying() { return playing;}
    void setPlaying(boolean val) {playing = val; }

    @Override
    public void onDraw(Canvas canvas) {
        //super.onDraw(canvas);
        int right = this.getRight();
        int left = this.getLeft();
        int bottom = this.getBottom();
        int top = this.getTop();
        float moveWhenPushed = 1.0f;
        float inset = 1; // a margin to prevent clipping the shape
        float size = Math.min(right - left, bottom - top) - moveWhenPushed - inset;
        float delta = inset + (getButtonState() == BtnState.Pushed || getPlaying() ? moveWhenPushed  : 0f);
        Path arrow = new Path();
        arrow.moveTo(delta, delta);
        arrow.lineTo(delta, (float) size + delta);
        arrow.lineTo((float) size, size / 2 + delta);
        arrow.lineTo(delta,delta);
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
