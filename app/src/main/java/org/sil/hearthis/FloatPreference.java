package org.sil.hearthis;

import android.content.Context;
import android.text.InputType;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;

public class FloatPreference extends EditTextPreference {
    public FloatPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Use the lambda here to restrict input to numbers/decimals
        setOnBindEditTextListener(editText ->
                editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL));
    }

    @Override
    protected void onSetInitialValue(@Nullable Object defaultValue) {
        float value;
        if (defaultValue instanceof String) {
            value = getPersistedFloat(Float.parseFloat((String) defaultValue));
        } else {
            value = getPersistedFloat(1.0f);
        }
        setText(String.valueOf(value));
    }

    @Override
    protected boolean persistString(String value) {
        try {
            // Save as float so SharedPreferences.getFloat() works elsewhere
            return persistFloat(Float.parseFloat(value));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        float defaultFloat = 1.0f;
        try {
            if (defaultReturnValue != null) {
                defaultFloat = Float.parseFloat(defaultReturnValue);
            }
        } catch (NumberFormatException ignored) {}
        return String.valueOf(getPersistedFloat(defaultFloat));
    }
}