package com.nastech.nasux.shared.nasux.theme;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nastech.nasux.shared.nasux.settings.properties.NasUXPropertyConstants;
import com.nastech.nasux.shared.nasux.settings.properties.NasUXSharedProperties;
import com.nastech.nasux.shared.theme.NightMode;

public class NasUXThemeUtils {

    /** Get the {@link NasUXPropertyConstants#KEY_NIGHT_MODE} value from the properties file on disk
     * and set it to app wide night mode value. */
    public static void setAppNightMode(@NonNull Context context) {
        NightMode.setAppNightMode(NasUXSharedProperties.getNightMode(context));
    }

    /** Set name as app wide night mode value. */
    public static void setAppNightMode(@Nullable String name) {
        NightMode.setAppNightMode(name);
    }

}
