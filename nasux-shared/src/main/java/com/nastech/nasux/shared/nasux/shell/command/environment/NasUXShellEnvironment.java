package com.nastech.nasux.shared.nasux.shell.command.environment;

import android.content.Context;

import androidx.annotation.NonNull;

import com.nastech.nasux.shared.errors.Error;
import com.nastech.nasux.shared.file.FileUtils;
import com.nastech.nasux.shared.logger.Logger;
import com.nastech.nasux.shared.shell.command.ExecutionCommand;
import com.nastech.nasux.shared.shell.command.environment.AndroidShellEnvironment;
import com.nastech.nasux.shared.shell.command.environment.ShellEnvironmentUtils;
import com.nastech.nasux.shared.shell.command.environment.ShellCommandShellEnvironment;
import com.nastech.nasux.shared.nasux.NasUXBootstrap;
import com.nastech.nasux.shared.nasux.NasUXConstants;
import com.nastech.nasux.shared.nasux.shell.NasUXShellUtils;

import java.nio.charset.Charset;
import java.util.HashMap;

/**
 * Environment for NasUX.
 */
public class NasUXShellEnvironment extends AndroidShellEnvironment {

    private static final String LOG_TAG = "NasUXShellEnvironment";

    /** Environment variable for the nasux {@link NasUXConstants#NASUX_PREFIX_DIR_PATH}. */
    public static final String ENV_PREFIX = "PREFIX";

    public NasUXShellEnvironment() {
        super();
        shellCommandShellEnvironment = new NasUXShellCommandShellEnvironment();
    }


    /** Init {@link NasUXShellEnvironment} constants and caches. */
    public synchronized static void init(@NonNull Context currentPackageContext) {
        NasUXAppShellEnvironment.setNasUXAppEnvironment(currentPackageContext);
    }

    /** Init {@link NasUXShellEnvironment} constants and caches. */
    public synchronized static void writeEnvironmentToFile(@NonNull Context currentPackageContext) {
        HashMap<String, String> environmentMap = new NasUXShellEnvironment().getEnvironment(currentPackageContext, false);
        String environmentString = ShellEnvironmentUtils.convertEnvironmentToDotEnvFile(environmentMap);

        // Write environment string to temp file and then move to final location since otherwise
        // writing may happen while file is being sourced/read
        Error error = FileUtils.writeTextToFile("nasux.env.tmp", NasUXConstants.NASUX_ENV_TEMP_FILE_PATH,
            Charset.defaultCharset(), environmentString, false);
        if (error != null) {
            Logger.logErrorExtended(LOG_TAG, error.toString());
            return;
        }

        error = FileUtils.moveRegularFile("nasux.env.tmp", NasUXConstants.NASUX_ENV_TEMP_FILE_PATH, NasUXConstants.NASUX_ENV_FILE_PATH, true);
        if (error != null) {
            Logger.logErrorExtended(LOG_TAG, error.toString());
        }
    }

    /** Get shell environment for NasUX. */
    @NonNull
    @Override
    public HashMap<String, String> getEnvironment(@NonNull Context currentPackageContext, boolean isFailSafe) {

        // NasUX environment builds upon the Android environment
        HashMap<String, String> environment = super.getEnvironment(currentPackageContext, isFailSafe);

        HashMap<String, String> nasuxAppEnvironment = NasUXAppShellEnvironment.getEnvironment(currentPackageContext);
        if (nasuxAppEnvironment != null)
            environment.putAll(nasuxAppEnvironment);

        HashMap<String, String> nasuxApiAppEnvironment = NasUXAPIShellEnvironment.getEnvironment(currentPackageContext);
        if (nasuxApiAppEnvironment != null)
            environment.putAll(nasuxApiAppEnvironment);

        environment.put(ENV_HOME, NasUXConstants.NASUX_HOME_DIR_PATH);
        environment.put(ENV_PREFIX, NasUXConstants.NASUX_PREFIX_DIR_PATH);

        // If failsafe is not enabled, then we keep default PATH and TMPDIR so that system binaries can be used
        if (!isFailSafe) {
            environment.put(ENV_TMPDIR, NasUXConstants.NASUX_TMP_PREFIX_DIR_PATH);
            if (NasUXBootstrap.isAppPackageVariantAPTAndroid5()) {
                // NasUX in android 5/6 era shipped busybox binaries in applets directory
                environment.put(ENV_PATH, NasUXConstants.NASUX_BIN_PREFIX_DIR_PATH + ":" + NasUXConstants.NASUX_BIN_PREFIX_DIR_PATH + "/applets");
                environment.put(ENV_LD_LIBRARY_PATH, NasUXConstants.NASUX_LIB_PREFIX_DIR_PATH);
            } else {
                // NasUX bootstrap ZIPs are patched from the upstream source which used a different
                // package path. LD_LIBRARY_PATH ensures the dynamic linker finds libs at the
                // NasUX path regardless of any embedded DT_RUNPATH in individual ELF binaries.
                environment.put(ENV_PATH, NasUXConstants.NASUX_BIN_PREFIX_DIR_PATH);
                environment.put(ENV_LD_LIBRARY_PATH, NasUXConstants.NASUX_LIB_PREFIX_DIR_PATH);
            }
        }

        return environment;
    }


    @NonNull
    @Override
    public String getDefaultWorkingDirectoryPath() {
        return NasUXConstants.NASUX_HOME_DIR_PATH;
    }

    @NonNull
    @Override
    public String getDefaultBinPath() {
        return NasUXConstants.NASUX_BIN_PREFIX_DIR_PATH;
    }

    @NonNull
    @Override
    public String[] setupShellCommandArguments(@NonNull String executable, String[] arguments) {
        return NasUXShellUtils.setupShellCommandArguments(executable, arguments);
    }

}
