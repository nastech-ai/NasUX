package com.nastech.nasux.app.fragments.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.nastech.nasux.R;
import com.nastech.nasux.shared.nasux.settings.preferences.NasUXAPIAppSharedPreferences;

@Keep
public class NasUXAPIPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(NasUXAPIPreferencesDataStore.getInstance(context));

        setPreferencesFromResource(R.xml.nasux_api_preferences, rootKey);
    }

}

class NasUXAPIPreferencesDataStore extends PreferenceDataStore {

    private final Context mContext;
    private final NasUXAPIAppSharedPreferences mPreferences;

    private static NasUXAPIPreferencesDataStore mInstance;

    private NasUXAPIPreferencesDataStore(Context context) {
        mContext = context;
        mPreferences = NasUXAPIAppSharedPreferences.build(context, true);
    }

    public static synchronized NasUXAPIPreferencesDataStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new NasUXAPIPreferencesDataStore(context);
        }
        return mInstance;
    }

}
