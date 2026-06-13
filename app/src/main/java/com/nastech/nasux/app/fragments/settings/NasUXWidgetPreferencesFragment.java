package com.nastech.nasux.app.fragments.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.nastech.nasux.R;
import com.nastech.nasux.shared.nasux.settings.preferences.NasUXWidgetAppSharedPreferences;

@Keep
public class NasUXWidgetPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(NasUXWidgetPreferencesDataStore.getInstance(context));

        setPreferencesFromResource(R.xml.nasux_widget_preferences, rootKey);
    }

}

class NasUXWidgetPreferencesDataStore extends PreferenceDataStore {

    private final Context mContext;
    private final NasUXWidgetAppSharedPreferences mPreferences;

    private static NasUXWidgetPreferencesDataStore mInstance;

    private NasUXWidgetPreferencesDataStore(Context context) {
        mContext = context;
        mPreferences = NasUXWidgetAppSharedPreferences.build(context, true);
    }

    public static synchronized NasUXWidgetPreferencesDataStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new NasUXWidgetPreferencesDataStore(context);
        }
        return mInstance;
    }

}
