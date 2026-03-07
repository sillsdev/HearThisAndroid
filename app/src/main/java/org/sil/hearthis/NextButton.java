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
public class NextButton extends CustomButton {
    public NextButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        blueFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        blueFillPaint.setColor(ContextCompat.getColor(context, R.color.audioButtonBlueColor));
        highlightBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightBorderPaint.setColor(ContextCompat.getColor(context, R.color.buttonSuggestedBorderColor));
        highlightBorderPaint.setStrokeWidth(4f);
        highlightBorderPaint.setStyle(Paint.Style.STROKE);

        waitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        waitPaint.setColor(ContextCompat.getColor(context, R.color.buttonWaitingColor));

        playBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        playBorderPaint.setColor(ContextCompat.getColor(context, R.color.buttonSuggestedBorderColor));
        playBorderPaint.setStrokeWidth(6f);
        playBorderPaint.setStyle(Paint.Style.STROKE);

        disabledPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        disabledPaint.setColor(ContextCompat.getColor(context, R.color.audioButtonDisabledColor));
    }

    final Paint blueFillPaint;
    final Paint highlightBorderPaint;
    final Paint waitPaint;
    final Paint disabledPaint;
    final Paint playBorderPaint;
    private final Path arrow = new Path();

    @Override
    public void onDraw(Canvas canvas) {
        //super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        float moveWhenPushed = 3.0f;
        float inset = 1; // a margin to prevent clipping the shape.
        float size = Math.min(w, h) - moveWhenPushed - inset;
        float thick = size/3;
        float deltaX = (w - size) / 2f + (getButtonState() == BtnState.Pushed ? moveWhenPushed : 0f);
        float deltaY = (h - size) / 2f + (getButtonState() == BtnState.Pushed ? moveWhenPushed : 0f);
        float midX = size / 2 + deltaX;
        float stemY = size * 12/33 + deltaY;

        arrow.moveTo(midX + thick / 2, deltaY); // upper right corner of stem
        arrow.lineTo(midX - thick / 2, deltaY); // upper left corner of stem
        arrow.lineTo(midX - thick / 2, stemY); // left junction of stem and arrow
        arrow.lineTo(deltaX, stemY); // left point of arrow
        arrow.lineTo(size/2 + deltaX, size + deltaY); // tip of arrow
        arrow.lineTo(size + deltaX, stemY); // right point of arrow
        arrow.lineTo(midX + thick / 2, stemY); // right junction of stem and arrow
        arrow.lineTo(midX + thick / 2, deltaY); // back to start

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
    }}
