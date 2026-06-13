package com.nastech.nasux.app.activities;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.nastech.nasux.R;
import com.nastech.nasux.shared.activities.ReportActivity;
import com.nastech.nasux.shared.file.FileUtils;
import com.nastech.nasux.shared.models.ReportInfo;
import com.nastech.nasux.app.models.UserAction;
import com.nastech.nasux.shared.interact.ShareUtils;
import com.nastech.nasux.shared.android.PackageUtils;
import com.nastech.nasux.shared.nasux.settings.preferences.NasUXAPIAppSharedPreferences;
import com.nastech.nasux.shared.nasux.settings.preferences.NasUXFloatAppSharedPreferences;
import com.nastech.nasux.shared.nasux.settings.preferences.NasUXTaskerAppSharedPreferences;
import com.nastech.nasux.shared.nasux.settings.preferences.NasUXWidgetAppSharedPreferences;
import com.nastech.nasux.shared.android.AndroidUtils;
import com.nastech.nasux.shared.nasux.NasUXConstants;
import com.nastech.nasux.shared.nasux.NasUXUtils;
import com.nastech.nasux.shared.activity.media.AppCompatActivityUtils;
import com.nastech.nasux.shared.theme.NightMode;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppCompatActivityUtils.setNightMode(this, NightMode.getAppNightMode().getName(), true);

        setContentView(R.layout.activity_settings);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new RootPreferencesFragment())
                .commit();
        }

        AppCompatActivityUtils.setToolbar(this, com.nastech.nasux.shared.R.id.toolbar);
        AppCompatActivityUtils.setShowBackButtonInActionBar(this, true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public static class RootPreferencesFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            Context context = getContext();
            if (context == null) return;

            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            new Thread() {
                @Override
                public void run() {
                    configureNasUXAPIPreference(context);
                    configureNasUXFloatPreference(context);
                    configureNasUXTaskerPreference(context);
                    configureNasUXWidgetPreference(context);
                    configureAboutPreference(context);
                    configureDonatePreference(context);
                }
            }.start();
        }

        private void configureNasUXAPIPreference(@NonNull Context context) {
            Preference nasuxAPIPreference = findPreference("nasux_api");
            if (nasuxAPIPreference != null) {
                NasUXAPIAppSharedPreferences preferences = NasUXAPIAppSharedPreferences.build(context, false);
                // If failed to get app preferences, then likely app is not installed, so do not show its preference
                nasuxAPIPreference.setVisible(preferences != null);
            }
        }

        private void configureNasUXFloatPreference(@NonNull Context context) {
            Preference nasuxFloatPreference = findPreference("nasux_float");
            if (nasuxFloatPreference != null) {
                NasUXFloatAppSharedPreferences preferences = NasUXFloatAppSharedPreferences.build(context, false);
                // If failed to get app preferences, then likely app is not installed, so do not show its preference
                nasuxFloatPreference.setVisible(preferences != null);
            }
        }

        private void configureNasUXTaskerPreference(@NonNull Context context) {
            Preference nasuxTaskerPreference = findPreference("nasux_tasker");
            if (nasuxTaskerPreference != null) {
                NasUXTaskerAppSharedPreferences preferences = NasUXTaskerAppSharedPreferences.build(context, false);
                // If failed to get app preferences, then likely app is not installed, so do not show its preference
                nasuxTaskerPreference.setVisible(preferences != null);
            }
        }

        private void configureNasUXWidgetPreference(@NonNull Context context) {
            Preference nasuxWidgetPreference = findPreference("nasux_widget");
            if (nasuxWidgetPreference != null) {
                NasUXWidgetAppSharedPreferences preferences = NasUXWidgetAppSharedPreferences.build(context, false);
                // If failed to get app preferences, then likely app is not installed, so do not show its preference
                nasuxWidgetPreference.setVisible(preferences != null);
            }
        }

        private void configureAboutPreference(@NonNull Context context) {
            Preference aboutPreference = findPreference("about");
            if (aboutPreference != null) {
                aboutPreference.setOnPreferenceClickListener(preference -> {
                    new Thread() {
                        @Override
                        public void run() {
                            String title = "About";

                            StringBuilder aboutString = new StringBuilder();
                            aboutString.append(NasUXUtils.getAppInfoMarkdownString(context, NasUXUtils.AppInfoMode.NASUX_AND_PLUGIN_PACKAGES));
                            aboutString.append("\n\n").append(AndroidUtils.getDeviceInfoMarkdownString(context, true));
                            aboutString.append("\n\n").append(NasUXUtils.getImportantLinksMarkdownString(context));

                            String userActionName = UserAction.ABOUT.getName();

                            ReportInfo reportInfo = new ReportInfo(userActionName,
                                NasUXConstants.NASUX_APP.NASUX_SETTINGS_ACTIVITY_NAME, title);
                            reportInfo.setReportString(aboutString.toString());
                            reportInfo.setReportSaveFileLabelAndPath(userActionName,
                                Environment.getExternalStorageDirectory() + "/" +
                                    FileUtils.sanitizeFileName(NasUXConstants.NASUX_APP_NAME + "-" + userActionName + ".log", true, true));

                            ReportActivity.startReportActivity(context, reportInfo);
                        }
                    }.start();

                    return true;
                });
            }
        }

        private void configureDonatePreference(@NonNull Context context) {
            Preference donatePreference = findPreference("donate");
            if (donatePreference != null) {
                String signingCertificateSHA256Digest = PackageUtils.getSigningCertificateSHA256DigestForPackage(context);
                if (signingCertificateSHA256Digest != null) {
                    // If APK is a Google Playstore release, then do not show the donation link
                    // since NasUX isn't exempted from the playstore policy donation links restriction
                    // Check Fund solicitations: https://pay.google.com/intl/en_in/about/policy/
                    String apkRelease = NasUXUtils.getAPKRelease(signingCertificateSHA256Digest);
                    if (apkRelease == null || apkRelease.equals(NasUXConstants.APK_RELEASE_GOOGLE_PLAYSTORE_SIGNING_CERTIFICATE_SHA256_DIGEST)) {
                        donatePreference.setVisible(false);
                        return;
                    } else {
                        donatePreference.setVisible(true);
                    }
                }

                donatePreference.setOnPreferenceClickListener(preference -> {
                    ShareUtils.openUrl(context, NasUXConstants.NASUX_DONATE_URL);
                    return true;
                });
            }
        }
    }

}
