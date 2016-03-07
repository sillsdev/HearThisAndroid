package org.sil.hearthis;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;

/**
 * Created by Thomson on 3/6/2016.
 */
public class NextButton extends CustomButton {
    public NextButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        blueFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        blueFillPaint.setColor(context.getResources().getColor(R.color.audioButtonBlueColor));
        highlightBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightBorderPaint.setColor(context.getResources().getColor(R.color.buttonSuggestedBorderColor));
        highlightBorderPaint.setStrokeWidth(4f);
        highlightBorderPaint.setStyle(Paint.Style.STROKE);

        waitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        waitPaint.setColor(context.getResources().getColor(R.color.buttonWaitingColor));

        playBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        playBorderPaint.setColor(context.getResources().getColor(R.color.buttonSuggestedBorderColor));
        playBorderPaint.setStrokeWidth(6f);
        playBorderPaint.setStyle(Paint.Style.STROKE);
    }

    Paint blueFillPaint;
    Paint highlightBorderPaint;
    Paint waitPaint;
    Paint disabledPaint;
    Paint playBorderPaint;

    @Override
    public void onDraw(Canvas canvas) {
        //super.onDraw(canvas);
        int right = this.getRight();
        int left = this.getLeft();
        int bottom = this.getBottom();
        int top = this.getTop();
        float moveWhenPushed = 3.0f;
        float inset = 1; // a margin to prevent clipping the shape.
        float size = Math.min(right - left, bottom - top) - moveWhenPushed - inset;
        float thick = size/3;
        float delta = inset + (getButtonState() == BtnState.Pushed ? moveWhenPushed : 0f);
        float mid = size / 2 + delta;
        float stem = size * 12/33 + delta;

        Path arrow = new Path();
        arrow.moveTo(delta, mid - thick / 2); // upper left corner of stem
        arrow.lineTo(delta, mid + thick / 2); // lower left corner of stem
        arrow.lineTo(stem, mid + thick/2); // lower junction of stem and arrow
        arrow.lineTo(stem, size + delta); // lower point of arrow
        arrow.lineTo(size + delta, size/2 + delta); // tip of arrow
        arrow.lineTo(stem, delta); // upper point of arrow
        arrow.lineTo(stem, mid - thick/2); // upper junction of stem and arrow
        arrow.lineTo(delta, mid - thick / 2); // back to start

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
