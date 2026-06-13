package com.nastech.nasux.app.fragments.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.nastech.nasux.R;
import com.nastech.nasux.shared.nasux.settings.preferences.NasUXFloatAppSharedPreferences;

@Keep
public class NasUXFloatPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(NasUXFloatPreferencesDataStore.getInstance(context));

        setPreferencesFromResource(R.xml.nasux_float_preferences, rootKey);
    }

}

class NasUXFloatPreferencesDataStore extends PreferenceDataStore {

    private final Context mContext;
    private final NasUXFloatAppSharedPreferences mPreferences;

    private static NasUXFloatPreferencesDataStore mInstance;

    private NasUXFloatPreferencesDataStore(Context context) {
        mContext = context;
        mPreferences = NasUXFloatAppSharedPreferences.build(context, true);
    }

    public static synchronized NasUXFloatPreferencesDataStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new NasUXFloatPreferencesDataStore(context);
        }
        return mInstance;
    }

}
