package org.sil.hearthis;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * This seems to be a necessary part of storing preferences in the approved way.
 * We don't yet actually launch this activity anywhere.
 * Created by Thomson on 3/8/2016.
 */
public class HearThisPreferences  extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}