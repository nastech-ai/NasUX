package com.nastech.nasux.app.fragments.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.nastech.nasux.R;
import com.nastech.nasux.shared.nasux.settings.preferences.NasUXAppSharedPreferences;

@Keep
public class NasUXPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(NasUXPreferencesDataStore.getInstance(context));

        setPreferencesFromResource(R.xml.nasux_preferences, rootKey);
    }

}

class NasUXPreferencesDataStore extends PreferenceDataStore {

    private final Context mContext;
    private final NasUXAppSharedPreferences mPreferences;

    private static NasUXPreferencesDataStore mInstance;

    private NasUXPreferencesDataStore(Context context) {
        mContext = context;
        mPreferences = NasUXAppSharedPreferences.build(context, true);
    }

    public static synchronized NasUXPreferencesDataStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new NasUXPreferencesDataStore(context);
        }
        return mInstance;
    }

}
