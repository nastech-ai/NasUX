package com.nastech.nasux.app.fragments.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.nastech.nasux.R;
import com.nastech.nasux.shared.nasux.settings.preferences.NasUXTaskerAppSharedPreferences;

@Keep
public class NasUXTaskerPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(NasUXTaskerPreferencesDataStore.getInstance(context));

        setPreferencesFromResource(R.xml.nasux_tasker_preferences, rootKey);
    }

}

class NasUXTaskerPreferencesDataStore extends PreferenceDataStore {

    private final Context mContext;
    private final NasUXTaskerAppSharedPreferences mPreferences;

    private static NasUXTaskerPreferencesDataStore mInstance;

    private NasUXTaskerPreferencesDataStore(Context context) {
        mContext = context;
        mPreferences = NasUXTaskerAppSharedPreferences.build(context, true);
    }

    public static synchronized NasUXTaskerPreferencesDataStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new NasUXTaskerPreferencesDataStore(context);
        }
        return mInstance;
    }

}
