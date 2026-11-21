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

    // Accepts a comma as the decimal separator (e.g. "1,5"), as typed on keyboards in many locales.
    static float parseLenient(String text) {
        float value = Float.parseFloat(text.trim().replace(',', '.'));
        // parseFloat accepts "NaN" and "Infinity"; neither is a usable scale.
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            throw new NumberFormatException("Not a finite number: " + text);
        }
        return value;
    }

    // Same limits LinesView applies to pinch-zoom, so typed values can't exceed what zooming allows.
    static final float MIN_VALUE = 0.5f;
    static final float MAX_VALUE = 5.0f;

    static float clamp(float value) {
        return Math.max(MIN_VALUE, Math.min(value, MAX_VALUE));
    }

    @Override
    protected void onSetInitialValue(@Nullable Object defaultValue) {
        float value;
        if (defaultValue instanceof String) {
            value = getPersistedFloat(parseLenient((String) defaultValue));
        } else {
            value = getPersistedFloat(1.0f);
        }
        setText(String.valueOf(value));
    }

    @Override
    protected boolean persistString(String value) {
        try {
            // Save as float so SharedPreferences.getFloat() works elsewhere
            float parsed = parseLenient(value);
            float clamped = clamp(parsed);
            if (clamped != parsed) {
                // Out of range: setText stores the clamped text (re-entering here, now in range)
                // and keeps the displayed value in sync with what was saved.
                setText(String.valueOf(clamped));
                return true;
            }
            return persistFloat(clamped);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        float defaultFloat = 1.0f;
        try {
            if (defaultReturnValue != null) {
                defaultFloat = parseLenient(defaultReturnValue);
            }
        } catch (NumberFormatException ignored) {}
        return String.valueOf(getPersistedFloat(defaultFloat));
    }
}