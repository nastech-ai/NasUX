package com.nastech.nasux.shared.nasux;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nastech.nasux.shared.logger.Logger;
import com.nastech.nasux.shared.nasux.NasUXConstants.NASUX_APP;

public class NasUXBootstrap {

    private static final String LOG_TAG = "NasUXBootstrap";

    /** The field name used by NasUX app to store package variant in
     * {@link NASUX_APP#BUILD_CONFIG_CLASS_NAME} class. */
    public static final String BUILD_CONFIG_FIELD_NASUX_PACKAGE_VARIANT = "NASUX_PACKAGE_VARIANT";


    /** The {@link PackageManager} for the bootstrap in the app APK added in app/build.gradle. */
    public static PackageManager NASUX_APP_PACKAGE_MANAGER;

    /** The {@link PackageVariant} for the bootstrap in the app APK added in app/build.gradle. */
    public static PackageVariant NASUX_APP_PACKAGE_VARIANT;

    /** Set {@link #NASUX_APP_PACKAGE_VARIANT} and {@link #NASUX_APP_PACKAGE_MANAGER} from {@code packageVariantName} passed. */
    public static void setNasUXPackageManagerAndVariant(@Nullable String packageVariantName) {
        NASUX_APP_PACKAGE_VARIANT = PackageVariant.variantOf(packageVariantName);
        if (NASUX_APP_PACKAGE_VARIANT == null) {
            throw new RuntimeException("Unsupported NASUX_APP_PACKAGE_VARIANT \"" + packageVariantName + "\"");
        }

        Logger.logVerbose(LOG_TAG, "Set NASUX_APP_PACKAGE_VARIANT to \"" + NASUX_APP_PACKAGE_VARIANT + "\"");

        // Set packageManagerName to substring before first dash "-" in packageVariantName
        int index = packageVariantName.indexOf('-');
        String packageManagerName = (index == -1) ? null : packageVariantName.substring(0, index);
        NASUX_APP_PACKAGE_MANAGER = PackageManager.managerOf(packageManagerName);
        if (NASUX_APP_PACKAGE_MANAGER == null) {
            throw new RuntimeException("Unsupported NASUX_APP_PACKAGE_MANAGER \"" + packageManagerName + "\" with variant \"" + packageVariantName + "\"");
        }

        Logger.logVerbose(LOG_TAG, "Set NASUX_APP_PACKAGE_MANAGER to \"" + NASUX_APP_PACKAGE_MANAGER + "\"");
    }

    /**
     * Set {@link #NASUX_APP_PACKAGE_VARIANT} and {@link #NASUX_APP_PACKAGE_MANAGER} with the
     * {@link #BUILD_CONFIG_FIELD_NASUX_PACKAGE_VARIANT} field value from the
     * {@link NASUX_APP#BUILD_CONFIG_CLASS_NAME} class of the NasUX app APK installed on the device.
     * This can only be used by apps that share `sharedUserId` with the NasUX app and can be used
     * by plugin apps.
     *
     * @param currentPackageContext The context of current package.
     */
    public static void setNasUXPackageManagerAndVariantFromNasUXApp(@NonNull Context currentPackageContext) {
        String packageVariantName = getNasUXAppBuildConfigPackageVariantFromNasUXApp(currentPackageContext);
        if (packageVariantName != null) {
            NasUXBootstrap.setNasUXPackageManagerAndVariant(packageVariantName);
        } else {
            Logger.logError(LOG_TAG, "Failed to set NASUX_APP_PACKAGE_VARIANT and NASUX_APP_PACKAGE_MANAGER from the nasux app");
        }
    }

    /**
     * Get {@link #BUILD_CONFIG_FIELD_NASUX_PACKAGE_VARIANT} field value from the
     * {@link NASUX_APP#BUILD_CONFIG_CLASS_NAME} class of the NasUX app APK installed on the device.
     * This can only be used by apps that share `sharedUserId` with the NasUX app.
     *
     * @param currentPackageContext The context of current package.
     * @return Returns the field value, otherwise {@code null} if an exception was raised or failed
     * to get nasux app package context.
     */
    public static String getNasUXAppBuildConfigPackageVariantFromNasUXApp(@NonNull Context currentPackageContext) {
        try {
            return (String) NasUXUtils.getNasUXAppAPKBuildConfigClassField(currentPackageContext, BUILD_CONFIG_FIELD_NASUX_PACKAGE_VARIANT);
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to get \"" + BUILD_CONFIG_FIELD_NASUX_PACKAGE_VARIANT + "\" value from \"" + NASUX_APP.BUILD_CONFIG_CLASS_NAME + "\" class", e);
            return null;
        }
    }



    /** Is {@link PackageManager#APT} set as {@link #NASUX_APP_PACKAGE_MANAGER}. */
    public static boolean isAppPackageManagerAPT() {
        return PackageManager.APT.equals(NASUX_APP_PACKAGE_MANAGER);
    }

    ///** Is {@link PackageManager#TAPM} set as {@link #NASUX_APP_PACKAGE_MANAGER}. */
    //public static boolean isAppPackageManagerTAPM() {
    //    return PackageManager.TAPM.equals(NASUX_APP_PACKAGE_MANAGER);
    //}

    ///** Is {@link PackageManager#PACMAN} set as {@link #NASUX_APP_PACKAGE_MANAGER}. */
    //public static boolean isAppPackageManagerPACMAN() {
    //    return PackageManager.PACMAN.equals(NASUX_APP_PACKAGE_MANAGER);
    //}



    /** Is {@link PackageVariant#APT_ANDROID_7} set as {@link #NASUX_APP_PACKAGE_VARIANT}. */
    public static boolean isAppPackageVariantAPTAndroid7() {
        return PackageVariant.APT_ANDROID_7.equals(NASUX_APP_PACKAGE_VARIANT);
    }

    /** Is {@link PackageVariant#APT_ANDROID_5} set as {@link #NASUX_APP_PACKAGE_VARIANT}. */
    public static boolean isAppPackageVariantAPTAndroid5() {
        return PackageVariant.APT_ANDROID_5.equals(NASUX_APP_PACKAGE_VARIANT);
    }

    ///** Is {@link PackageVariant#TAPM_ANDROID_7} set as {@link #NASUX_APP_PACKAGE_VARIANT}. */
    //public static boolean isAppPackageVariantTAPMAndroid7() {
    //    return PackageVariant.TAPM_ANDROID_7.equals(NASUX_APP_PACKAGE_VARIANT);
    //}

    ///** Is {@link PackageVariant#PACMAN_ANDROID_7} set as {@link #NASUX_APP_PACKAGE_VARIANT}. */
    //public static boolean isAppPackageVariantTPACMANAndroid7() {
    //    return PackageVariant.PACMAN_ANDROID_7.equals(NASUX_APP_PACKAGE_VARIANT);
    //}



    /** NasUX package manager. */
    public enum PackageManager {

        /**
         * Advanced Package Tool (APT) for managing debian deb package files.
         * https://wiki.debian.org/Apt
         * https://wiki.debian.org/deb
         */
        APT("apt");

        ///**
        // * NasUX Android Package Manager (TAPM) for managing nasux apk package files.
        // * https://en.wikipedia.org/wiki/Apk_(file_format)
        // */
        //TAPM("tapm");

        ///**
        // * Package Manager (PACMAN) for managing arch linux pkg.tar package files.
        // * https://wiki.archlinux.org/title/pacman
        // * https://en.wikipedia.org/wiki/Arch_Linux#Pacman
        // */
        //PACMAN("pacman");

        private final String name;

        PackageManager(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public boolean equalsManager(String manager) {
            return manager != null && manager.equals(this.name);
        }

        /** Get {@link PackageManager} for {@code name} if found, otherwise {@code null}. */
        @Nullable
        public static PackageManager managerOf(String name) {
            if (name == null || name.isEmpty()) return null;
            for (PackageManager v : PackageManager.values()) {
                if (v.name.equals(name)) {
                    return v;
                }
            }
            return null;
        }

    }



    /** NasUX package variant. The substring before first dash "-" must match one of the {@link PackageManager}. */
    public enum PackageVariant {

        /** {@link PackageManager#APT} variant for Android 7+. */
        APT_ANDROID_7("apt-android-7"),

        /** {@link PackageManager#APT} variant for Android 5+. */
        APT_ANDROID_5("apt-android-5");

        ///** {@link PackageManager#TAPM} variant for Android 7+. */
        //TAPM_ANDROID_7("tapm-android-7");

        ///** {@link PackageManager#PACMAN} variant for Android 7+. */
        //PACMAN_ANDROID_7("pacman-android-7");

        private final String name;

        PackageVariant(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public boolean equalsVariant(String variant) {
            return variant != null && variant.equals(this.name);
        }

        /** Get {@link PackageVariant} for {@code name} if found, otherwise {@code null}. */
        @Nullable
        public static PackageVariant variantOf(String name) {
            if (name == null || name.isEmpty()) return null;
            for (PackageVariant v : PackageVariant.values()) {
                if (v.name.equals(name)) {
                    return v;
                }
            }
            return null;
        }

    }

}
