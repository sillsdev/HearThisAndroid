package org.sil.hearthis;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * This class implements a LinearLayout which is expected to contain (only) a single ScrollView
 * containing a LinearLayout with asequence of TextViews.
 * It allows the user to grow or shrink the text in the child views
 * using the pinch guesture.
 * Currently it has direct knowledge that the scale factor is persisted in a setting called text_scale.
 * Client should call updateScale() after changing the sequence of child views.
 * Enhance: probably could be made to automatically set scale on any child added
 * Name of preference to save scale could be configured.
 * Possibly it would be better to persist the font size (in case we want to let the user edit directly)
 * Created by Thomson on 3/8/2016.
 *
 * Note: originally this was implemented as a replacement for the LinearLayout directly containing the
 * text views, inside the scroll view. This doesn't work as well because often the scroll view
 * captures a touch and tries to scroll, when the user is trying to zoom. With the view managing
 * the zoom outside the scroll view, the ScaleGestureDetector gets first dibs on the touch,
 * and can cancel the scroll if it detects a second touch.
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
        ViewGroup textViewHolder = getTextViewHolder();
        if (textViewHolder.getChildCount() > 0) {
            TextView firstLine = (TextView) textViewHolder.getChildAt(0);
            originalTextSize = firstLine.getTextSize();
        }
    }

    private ViewGroup getTextViewHolder() {
        ViewGroup parent = this;
        while (parent.getChildCount() > 0) {
            View firstChild = parent.getChildAt(0);
            if (firstChild instanceof ViewGroup) {
                parent = (ViewGroup) firstChild;
            }
            else {
                break;
            }
        }
        return parent;
    }

    // Update all the children to the current (possibly from preferences) scale
    public void updateScale() {
        if (originalTextSize == 0) {
            captureOriginalFontSize(); // have to get this before any updating takes place.
        }
        ViewGroup textViewHolder = getTextViewHolder();
        float textSize = originalTextSize * scale;
        for (int i = 0; i < textViewHolder.getChildCount(); i++) {
            TextView line = (TextView)textViewHolder.getChildAt(i);
            line.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        }
    }

//    @Override
//    public boolean dispatchTouchEvent(MotionEvent ev) {
//        scaleManager.onTouchEvent(ev);
//        if (scaleManager.isInProgress()) {
//
//        }
//        super.dispatchTouchEvent(ev);
//        return
//    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // We want all touch events to go to the scaleManager; otherwise, there's no way
        // it can ever notice the start fo a pinch. It is supposed to return a result saying
        // whether it wants future events for this touch, but
        // http://stackoverflow.com/questions/4575355/scalegesturedetector-ontouchevent-always-returns-true
        // says in practice ScaleGestureDetector.onTouchEvent ALWAYS returns true. So ignore the result.
        scaleManager.onTouchEvent(ev);
        skipOneTouch = scaleManager.isInProgress();
        // Returning true means tat THIS event and all future events for this touch will go
        // to OUR onTouchEvent method rather than to children. If a pinch is started, it's a
        // good thing to suppress events to children, since it seems to make the pinch work a
        // bit more reliably, though it doesn't reliably stop the first touch from selecting
        // something.
        // However, returning true locks in interception for this touch, so this method will not
        // be called for future events of the touch. But the scaleManager needs those events!
        // Therefore, typically we must call the scaleManager.onTouchEvent in our own
        // onTouchEvent also.
        // But, this current event will also be sent to onTouchEvent when we return true,
        // and we already passed IT to the scaleManager. So we want onTouchEvent to skip
        // passing the very next event it sees (which should be the one we alerady passed)
        // to the scale manager.
        return skipOneTouch;
    }

    boolean skipOneTouch = false;

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // Skip the event we already passed to the scale manager that resulted in its being
        // in progress. After that, send events to it until it is no longer in progress.
        // Seems it might be helpful to only pass on events if scaleManager.isInProgress(),
        // to prevent handling twice any event that was already passed to scaleManager
        // in onInterceptTouchEvent and is not consumed by a child. But somehow adding that
        // test kills pinch detection altogether.
        if (skipOneTouch) {
            skipOneTouch = false;
        } else {
            scaleManager.onTouchEvent(ev);
        }
        return true;
    }
}
