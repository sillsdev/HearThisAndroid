package org.sil.hearthis;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

/**
 * Created by Thomson on 3/5/2016.
 * Parent class for audio control buttons.
 */
public class CustomButton extends Button {
    public CustomButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    BtnState buttonState = BtnState.Normal;

    public void setButtonState(BtnState state) {
        buttonState = state;
        invalidate();
    }

    public BtnState getButtonState() {
        return buttonState;
    }

    boolean isDefault = false;

    public boolean getIsDefault() { return isDefault;}
    public void setIsDefault(boolean val) {
        isDefault = val;
        invalidate();
    }
}

