package com.nastech.nasux.shared.nasux.settings.properties;

import android.content.Context;

import androidx.annotation.NonNull;

import com.nastech.nasux.shared.nasux.NasUXConstants;

public class NasUXAppSharedProperties extends NasUXSharedProperties {

    private static NasUXAppSharedProperties properties;


    private NasUXAppSharedProperties(@NonNull Context context) {
        super(context, NasUXConstants.NASUX_APP_NAME,
            NasUXConstants.NASUX_PROPERTIES_FILE_PATHS_LIST, NasUXPropertyConstants.NASUX_APP_PROPERTIES_LIST,
            new NasUXSharedProperties.SharedPropertiesParserClient());
    }

    /**
     * Initialize the {@link #properties} and load properties from disk.
     *
     * @param context The {@link Context} for operations.
     * @return Returns the {@link NasUXAppSharedProperties}.
     */
    public static NasUXAppSharedProperties init(@NonNull Context context) {
        if (properties == null)
            properties = new NasUXAppSharedProperties(context);

        return properties;
    }

    /**
     * Get the {@link #properties}.
     *
     * @return Returns the {@link NasUXAppSharedProperties}.
     */
    public static NasUXAppSharedProperties getProperties() {
        return properties;
    }

}
