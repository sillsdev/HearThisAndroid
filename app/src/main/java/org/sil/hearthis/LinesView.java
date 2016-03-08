package org.sil.hearthis;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * This class implements a LinearLayout which is expected to contain (only) a sequence of TextViews.
 * It enhances LinearLayout by allowing the user to grow or shrink the text in the child views
 * using the pinch guesture.
 * Currently it has direct knowledge that the scale factor is persisted in a setting called text_scale.
 * Client should call updateScale() after changing the sequence of child views.
 * Enhance: probably could be made to automatically set scale on any child added
 * Name of preference to save scale could be configured.
 * Possibly it would be better to persist the font size (in case we want to let the user edit directly)
 * Created by Thomson on 3/8/2016.
 */
public class LinesView extends LinearLayout {

    private ScaleGestureDetector scaleManager;
    public float scale = 1f;
    float originalTextSize;

    public LinesView(Context context, AttributeSet attrs) {
        super(context, attrs);
        scaleManager = new ScaleGestureDetector(getContext(), new ScaleListener());
        scale = PreferenceManager.getDefaultSharedPreferences(getContext()).getFloat("text_scale", 1f);
    }

    private class ScaleListener extends ScaleGestureDetector.

            SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scale *= detector.getScaleFactor();
            scale = Math.max(0.5f, Math.min(scale, 5.0f));
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
            editor.putFloat("text_scale", scale);
            editor.commit();
            updateScale();
            return true;
        }
    }

    // Call this AFTER adding some text views and BEFORE updateScale
    public void captureOriginalFontSize() {
        if (getChildCount() > 0) {
            TextView firstLine = (TextView)getChildAt(0);
            originalTextSize = firstLine.getTextSize();
        }
    }

    // Update all the children to the current (possibly from preferences) scale
    public void updateScale() {
        if (originalTextSize == 0) {
            captureOriginalFontSize(); // have to get this before any updating takes place.
        }
        float textSize = originalTextSize * scale;
        for (int i = 0; i < getChildCount(); i++) {
            TextView line = (TextView)getChildAt(i);
            line.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev){
        super.dispatchTouchEvent(ev);
        return scaleManager.onTouchEvent(ev);
    }

    //Seems like a more logical approach but doesn't work.
    // Suggestion to override dispatchTouchEvent came from a stackOverflow article I have lost.
//    @Override
//    public boolean onInterceptTouchEvent(MotionEvent ev) {
//        scaleManager.onTouchEvent(ev);
//        // for now let the event go on to the child.
//        //  Eventually somehow we may be able to prevent this if it is a pinch.
//        return false;
//    }
}
