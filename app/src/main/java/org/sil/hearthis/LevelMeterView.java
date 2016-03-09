package org.sil.hearthis;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.Date;

/**
 * Created by Thomson on 3/3/2016.
 */
public class LevelMeterView extends View {
    Paint backgroundPaint;
    Paint goodLevelPaint;
    Paint badLevelPaint;

    public LevelMeterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    void init(Context context) {
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(context.getResources().getColor(R.color.mainBackground));
        goodLevelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        goodLevelPaint.setColor(context.getResources().getColor(R.color.goodLevelColor));
        badLevelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        badLevelPaint.setColor(context.getResources().getColor(R.color.badLevelColor));
    }

    int maxLevelSinceLastUpdate; // percent
    int displayLevel;
    Date lastUpdate = new Date();
    public void setLevel(int newLevel) {
        maxLevelSinceLastUpdate = Math.max(newLevel, maxLevelSinceLastUpdate);
        Date now = new Date();
        if (now.getTime() - lastUpdate.getTime() > 100) {
            lastUpdate = now;
            displayLevel = maxLevelSinceLastUpdate;
            maxLevelSinceLastUpdate = newLevel;
            this.invalidate();
        }
    }

    int ledCount = 20;
    int gapFraction = 5; // Divide space per LED by this to get gap width

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        Paint background = new Paint();
        canvas.drawRect(0.0F, 0.0F, (float)width, (float)height,backgroundPaint);
        int gap = width / ledCount  / gapFraction;
        int ledWidth = width / ledCount - gap;
        int ledsToShow = Math.round((float)ledCount * displayLevel / 100);
        int top = height / 6;
        int barHeight = height - 2 * top;
        for (int i = 0; i < ledsToShow; i++) {
            int left = i * (gap + ledWidth);
            Paint howToPaint = goodLevelPaint;
            // Top 3db is dangerous...about 2 leds.
            if (i >= ledCount - 2) {
                howToPaint = badLevelPaint;
            }
            canvas.drawRect((float)left, (float) top, (float) ledWidth + left, (float) top + barHeight, howToPaint);
        }
    }
}
