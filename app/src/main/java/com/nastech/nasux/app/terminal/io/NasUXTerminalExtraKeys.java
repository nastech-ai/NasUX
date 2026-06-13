package com.nastech.nasux.app.terminal.io;

import android.annotation.SuppressLint;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;

import com.nastech.nasux.app.NasUXActivity;
import com.nastech.nasux.app.terminal.NasUXTerminalSessionActivityClient;
import com.nastech.nasux.app.terminal.NasUXTerminalViewClient;
import com.nastech.nasux.shared.logger.Logger;
import com.nastech.nasux.shared.nasux.extrakeys.ExtraKeysConstants;
import com.nastech.nasux.shared.nasux.extrakeys.ExtraKeysInfo;
import com.nastech.nasux.shared.nasux.settings.properties.NasUXPropertyConstants;
import com.nastech.nasux.shared.nasux.settings.properties.NasUXSharedProperties;
import com.nastech.nasux.shared.nasux.terminal.io.TerminalExtraKeys;
import com.nastech.nasux.view.TerminalView;

import org.json.JSONException;

public class NasUXTerminalExtraKeys extends TerminalExtraKeys {

    private ExtraKeysInfo mExtraKeysInfo;

    final NasUXActivity mActivity;
    final NasUXTerminalViewClient mNasUXTerminalViewClient;
    final NasUXTerminalSessionActivityClient mNasUXTerminalSessionActivityClient;

    private static final String LOG_TAG = "NasUXTerminalExtraKeys";

    public NasUXTerminalExtraKeys(NasUXActivity activity, @NonNull TerminalView terminalView,
                                   NasUXTerminalViewClient nasuxTerminalViewClient,
                                   NasUXTerminalSessionActivityClient nasuxTerminalSessionActivityClient) {
        super(terminalView);

        mActivity = activity;
        mNasUXTerminalViewClient = nasuxTerminalViewClient;
        mNasUXTerminalSessionActivityClient = nasuxTerminalSessionActivityClient;

        setExtraKeys();
    }


    /**
     * Set the terminal extra keys and style.
     */
    private void setExtraKeys() {
        mExtraKeysInfo = null;

        try {
            // The mMap stores the extra key and style string values while loading properties
            // Check {@link #getExtraKeysInternalPropertyValueFromValue(String)} and
            // {@link #getExtraKeysStyleInternalPropertyValueFromValue(String)}
            String extrakeys = (String) mActivity.getProperties().getInternalPropertyValue(NasUXPropertyConstants.KEY_EXTRA_KEYS, true);
            String extraKeysStyle = (String) mActivity.getProperties().getInternalPropertyValue(NasUXPropertyConstants.KEY_EXTRA_KEYS_STYLE, true);

            ExtraKeysConstants.ExtraKeyDisplayMap extraKeyDisplayMap = ExtraKeysInfo.getCharDisplayMapForStyle(extraKeysStyle);
            if (ExtraKeysConstants.EXTRA_KEY_DISPLAY_MAPS.DEFAULT_CHAR_DISPLAY.equals(extraKeyDisplayMap) && !NasUXPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS_STYLE.equals(extraKeysStyle)) {
                Logger.logError(NasUXSharedProperties.LOG_TAG, "The style \"" + extraKeysStyle + "\" for the key \"" + NasUXPropertyConstants.KEY_EXTRA_KEYS_STYLE + "\" is invalid. Using default style instead.");
                extraKeysStyle = NasUXPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS_STYLE;
            }

            mExtraKeysInfo = new ExtraKeysInfo(extrakeys, extraKeysStyle, ExtraKeysConstants.CONTROL_CHARS_ALIASES);
        } catch (JSONException e) {
            Logger.showToast(mActivity, "Could not load and set the \"" + NasUXPropertyConstants.KEY_EXTRA_KEYS + "\" property from the properties file: " + e.toString(), true);
            Logger.logStackTraceWithMessage(LOG_TAG, "Could not load and set the \"" + NasUXPropertyConstants.KEY_EXTRA_KEYS + "\" property from the properties file: ", e);

            try {
                mExtraKeysInfo = new ExtraKeysInfo(NasUXPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS, NasUXPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS_STYLE, ExtraKeysConstants.CONTROL_CHARS_ALIASES);
            } catch (JSONException e2) {
                Logger.showToast(mActivity, "Can't create default extra keys",true);
                Logger.logStackTraceWithMessage(LOG_TAG, "Could create default extra keys: ", e);
                mExtraKeysInfo = null;
            }
        }
    }

    public ExtraKeysInfo getExtraKeysInfo() {
        return mExtraKeysInfo;
    }

    @SuppressLint("RtlHardcoded")
    @Override
    public void onTerminalExtraKeyButtonClick(View view, String key, boolean ctrlDown, boolean altDown, boolean shiftDown, boolean fnDown) {
        if ("KEYBOARD".equals(key)) {
            if(mNasUXTerminalViewClient != null)
                mNasUXTerminalViewClient.onToggleSoftKeyboardRequest();
        } else if ("DRAWER".equals(key)) {
            DrawerLayout drawerLayout = mNasUXTerminalViewClient.getActivity().getDrawer();
            if (drawerLayout.isDrawerOpen(Gravity.LEFT))
                drawerLayout.closeDrawer(Gravity.LEFT);
            else
                drawerLayout.openDrawer(Gravity.LEFT);
        } else if ("PASTE".equals(key)) {
            if(mNasUXTerminalSessionActivityClient != null)
                mNasUXTerminalSessionActivityClient.onPasteTextFromClipboard(null);
        }  else if ("SCROLL".equals(key)) {
            TerminalView terminalView = mNasUXTerminalViewClient.getActivity().getTerminalView();
            if (terminalView != null && terminalView.mEmulator != null)
                terminalView.mEmulator.toggleAutoScrollDisabled();
        } else {
            super.onTerminalExtraKeyButtonClick(view, key, ctrlDown, altDown, shiftDown, fnDown);
        }
    }

}
