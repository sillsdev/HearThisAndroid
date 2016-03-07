package org.sil.hearthis;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;

/**
 * Created by Thomson on 3/5/2016.
 */
public class RecordButton extends CustomButton {
    public RecordButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        blueFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        blueFillPaint.setColor(context.getResources().getColor(R.color.audioButtonBlueColor));
        highlightBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightBorderPaint.setColor(context.getResources().getColor(R.color.buttonSuggestedBorderColor));
        highlightBorderPaint.setStrokeWidth(4f);
        highlightBorderPaint.setStyle(Paint.Style.STROKE);

        waitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        waitPaint.setColor(context.getResources().getColor(R.color.buttonWaitingColor));
        recordingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        recordingPaint.setColor(context.getResources().getColor(R.color.recordingColor));
    }

    Paint blueFillPaint;
    Paint highlightBorderPaint;
    Paint waitPaint;
    Paint recordingPaint;
    private boolean waiting;

    public boolean getWaiting() { return waiting;}
    public void setWaiting(boolean val) {
        waiting = val;
        postInvalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        //super.onDraw(canvas);
        int right = this.getRight();
        int left = this.getLeft();
        int bottom = this.getBottom();
        int top = this.getTop();
        int dim = Math.min(right - left, bottom - top) - 2;
        float center = ((float)dim + 1)/2;
        float radius = ((float)dim)/2 - 1; // The extra -1 seems to be needed to prevent clipping the circle.

        switch (getButtonState())
        {
            case Normal:
                canvas.drawCircle(center, center, radius, blueFillPaint);
                if (getIsDefault())
                    canvas.drawCircle(center, center, radius, highlightBorderPaint);
                break;
            case Pushed:
                canvas.drawCircle(center, center, radius, getWaiting() ? waitPaint : recordingPaint);
                break;
            case Inactive: // not used
                //canvas.drawCircle(center, center, radius, disabledPaint);
                break;
        }
    }
}
