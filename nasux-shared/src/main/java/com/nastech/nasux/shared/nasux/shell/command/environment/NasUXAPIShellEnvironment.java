package com.nastech.nasux.shared.nasux.shell.command.environment;

import android.content.Context;
import android.content.pm.PackageInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nastech.nasux.shared.android.PackageUtils;
import com.nastech.nasux.shared.shell.command.environment.ShellEnvironmentUtils;
import com.nastech.nasux.shared.nasux.NasUXConstants;
import com.nastech.nasux.shared.nasux.NasUXUtils;

import java.util.HashMap;

/**
 * Environment for {@link NasUXConstants#NASUX_API_PACKAGE_NAME} app.
 */
public class NasUXAPIShellEnvironment {

    /** Environment variable prefix for the NasUX:API app. */
    public static final String NASUX_API_APP_ENV_PREFIX = NasUXConstants.NASUX_ENV_PREFIX_ROOT + "_API_APP__";

    /** Environment variable for the NasUX:API app version. */
    public static final String ENV_NASUX_API_APP__VERSION_NAME = NASUX_API_APP_ENV_PREFIX + "VERSION_NAME";

    /** Get shell environment for NasUX:API app. */
    @Nullable
    public static HashMap<String, String> getEnvironment(@NonNull Context currentPackageContext) {
        if (NasUXUtils.isNasUXAPIAppInstalled(currentPackageContext) != null) return null;

        String packageName = NasUXConstants.NASUX_API_PACKAGE_NAME;
        PackageInfo packageInfo = PackageUtils.getPackageInfoForPackage(currentPackageContext, packageName);
        if (packageInfo == null) return null;

        HashMap<String, String> environment = new HashMap<>();

        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_NASUX_API_APP__VERSION_NAME, PackageUtils.getVersionNameForPackage(packageInfo));

        return environment;
    }

}
