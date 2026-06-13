package com.nastech.nasux.shared.nasux.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.drawable.Icon;
import android.os.Build;

import androidx.annotation.Nullable;

import com.nastech.nasux.shared.R;
import com.nastech.nasux.shared.android.resource.ResourceUtils;
import com.nastech.nasux.shared.notification.NotificationUtils;
import com.nastech.nasux.shared.nasux.settings.preferences.NasUXAppSharedPreferences;
import com.nastech.nasux.shared.nasux.settings.preferences.NasUXPreferenceConstants;
import com.nastech.nasux.shared.nasux.NasUXConstants;

public class NasUXNotificationUtils {
    /**
     * Try to get the next unique notification id that isn't already being used by the app.
     *
     * NasUX app and its plugin must use unique notification ids from the same pool due to usage of android:sharedUserId.
     * https://commonsware.com/blog/2017/06/07/jobscheduler-job-ids-libraries.html
     *
     * @param context The {@link Context} for operations.
     * @return Returns the notification id that should be safe to use.
     */
    public synchronized static int getNextNotificationId(final Context context) {
        if (context == null) return NasUXPreferenceConstants.NASUX_APP.DEFAULT_VALUE_KEY_LAST_NOTIFICATION_ID;

        NasUXAppSharedPreferences preferences = NasUXAppSharedPreferences.build(context);
        if (preferences == null) return NasUXPreferenceConstants.NASUX_APP.DEFAULT_VALUE_KEY_LAST_NOTIFICATION_ID;

        int lastNotificationId = preferences.getLastNotificationId();

        int nextNotificationId = lastNotificationId + 1;
        while(nextNotificationId == NasUXConstants.NASUX_APP_NOTIFICATION_ID || nextNotificationId == NasUXConstants.NASUX_RUN_COMMAND_NOTIFICATION_ID) {
            nextNotificationId++;
        }

        if (nextNotificationId == Integer.MAX_VALUE || nextNotificationId < 0)
            nextNotificationId = NasUXPreferenceConstants.NASUX_APP.DEFAULT_VALUE_KEY_LAST_NOTIFICATION_ID;

        preferences.setLastNotificationId(nextNotificationId);
        return nextNotificationId;
    }

    /**
     * Get {@link Notification.Builder} for nasux app or its plugin.
     *
     * @param currentPackageContext The {@link Context} of current package.
     * @param nasuxPackageContext The {@link Context} of nasux package.
     * @param channelId The channel id for the notification.
     * @param priority The priority for the notification.
     * @param title The title for the notification.
     * @param notificationText The second line text of the notification.
     * @param notificationBigText The full text of the notification that may optionally be styled.
     * @param contentIntent The {@link PendingIntent} which should be sent when notification is clicked.
     * @param deleteIntent The {@link PendingIntent} which should be sent when notification is deleted.
     * @param notificationMode The notification mode. It must be one of {@code NotificationUtils.NOTIFICATION_MODE_*}.
     * @return Returns the {@link Notification.Builder}.
     */
    @Nullable
    public static Notification.Builder getNasUXOrPluginAppNotificationBuilder(final Context currentPackageContext,
                                                                                 final Context nasuxPackageContext,
                                                                                 final String channelId,
                                                                                 final int priority,
                                                                                 final CharSequence title,
                                                                                 final CharSequence notificationText,
                                                                                 final CharSequence notificationBigText,
                                                                                 final PendingIntent contentIntent,
                                                                                 final PendingIntent deleteIntent,
                                                                                 final int notificationMode) {
        Notification.Builder builder =  NotificationUtils.geNotificationBuilder(nasuxPackageContext,
            channelId, priority,
            title, notificationText, notificationBigText, contentIntent, deleteIntent, notificationMode);

        if (builder == null)  return null;

        // Enable timestamp
        builder.setShowWhen(true);

        // Set notification icon
        // If a notification is to be shown by a nasux plugin app, then we can't use the drawable
        // resource id for the plugin app with setSmallIcon(@DrawableRes int icon) since notification
        // is shown with nasuxPackageContext and nasux-app package would have a different id and
        // when android tries to load the drawable an exception would be thrown and notification will
        // not be thrown.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Set Icon instead of drawable resource id
            builder.setSmallIcon(Icon.createWithResource(currentPackageContext, R.drawable.ic_error_notification));
        } else {
            // Set drawable resource id used by nasux-app package
            Integer iconResId = ResourceUtils.getDrawableResourceId(nasuxPackageContext, "ic_error_notification",
                nasuxPackageContext.getPackageName(), true);
            if (iconResId != null)
                builder.setSmallIcon(iconResId);
        }

        // Set background color for small notification icon
        builder.setColor(0xFF607D8B);

        // Dismiss on click
        builder.setAutoCancel(true);

        return builder;
    }

}
