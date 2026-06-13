package com.nastech.nasux.shared.nasux.shell.am;

import android.content.Context;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nastech.nasux.shared.errors.Error;
import com.nastech.nasux.shared.logger.Logger;
import com.nastech.nasux.shared.net.socket.local.LocalClientSocket;
import com.nastech.nasux.shared.net.socket.local.LocalServerSocket;
import com.nastech.nasux.shared.net.socket.local.LocalSocketManager;
import com.nastech.nasux.shared.net.socket.local.LocalSocketManagerClientBase;
import com.nastech.nasux.shared.net.socket.local.LocalSocketRunConfig;
import com.nastech.nasux.shared.shell.am.AmSocketServerRunConfig;
import com.nastech.nasux.shared.shell.am.AmSocketServer;
import com.nastech.nasux.shared.nasux.NasUXConstants;
import com.nastech.nasux.shared.nasux.crash.NasUXCrashUtils;
import com.nastech.nasux.shared.nasux.plugins.NasUXPluginUtils;
import com.nastech.nasux.shared.nasux.settings.properties.NasUXAppSharedProperties;
import com.nastech.nasux.shared.nasux.settings.properties.NasUXPropertyConstants;
import com.nastech.nasux.shared.nasux.shell.command.environment.NasUXAppShellEnvironment;

/**
 * A wrapper for {@link AmSocketServer} for nasux-app usage.
 *
 * The static {@link #nasuxAmSocketServer} variable stores the {@link LocalSocketManager} for the
 * {@link AmSocketServer}.
 *
 * The {@link NasUXAmSocketServerClient} extends the {@link AmSocketServer.AmSocketServerClient}
 * class to also show plugin error notifications for errors and disallowed client connections in
 * addition to logging the messages to logcat, which are only logged by {@link LocalSocketManagerClientBase}
 * if log level is debug or higher for privacy issues.
 *
 * It uses a filesystem socket server with the socket file at
 * {@link NasUXConstants.NASUX_APP#NASUX_AM_SOCKET_FILE_PATH}. It would normally only allow
 * processes belonging to the nasux user and root user to connect to it. If commands are sent by the
 * root user, then the am commands executed will be run as the nasux user and its permissions,
 * capabilities and selinux context instead of root.
 *
 * The `$PREFIX/bin/nasux-am` client connects to the server via `$PREFIX/bin/nasux-am-socket` to
 * run the am commands. It provides similar functionality to "$PREFIX/bin/am"
 * (and "/system/bin/am"), but should be faster since it does not require starting a dalvik vm for
 * every command as done by "am" via nasux/NasUXAm.
 *
 * The server is started by nasux-app Application class but is not started if
 * {@link NasUXPropertyConstants#KEY_RUN_NASUX_AM_SOCKET_SERVER} is `false` which can be done by
 * adding the prop with value "false" to the "~/.nasux/nasux.properties" file. Changes
 * require nasux-app to be force stopped and restarted.
 *
 * The current state of the server can be checked with the
 * {@link NasUXAppShellEnvironment#ENV_NASUX_APP__AM_SOCKET_SERVER_ENABLED} env variable, which is exported
 * for all shell sessions and tasks.
 *
 * https://github.com/nasux/nasux-am-socket
 * https://github.com/nasux/NasUXAm
 */
public class NasUXAmSocketServer {

    public static final String LOG_TAG = "NasUXAmSocketServer";

    public static final String TITLE = "NasUXAm";

    /** The static instance for the {@link NasUXAmSocketServer} {@link LocalSocketManager}. */
    private static LocalSocketManager nasuxAmSocketServer;

    /** Whether {@link NasUXAmSocketServer} is enabled and running or not. */
    @Keep
    protected static Boolean NASUX_APP_AM_SOCKET_SERVER_ENABLED;

    /**
     * Setup the {@link AmSocketServer} {@link LocalServerSocket} and start listening for
     * new {@link LocalClientSocket} if enabled.
     *
     * @param context The {@link Context} for {@link LocalSocketManager}.
     */
    public static void setupNasUXAmSocketServer(@NonNull Context context) {
        // Start nasux-am-socket server if enabled by user
        boolean enabled = false;
        if (NasUXAppSharedProperties.getProperties().shouldRunNasUXAmSocketServer()) {
            Logger.logDebug(LOG_TAG, "Starting " + TITLE + " socket server since its enabled");
            start(context);
            if (nasuxAmSocketServer != null && nasuxAmSocketServer.isRunning()) {
                enabled = true;
                Logger.logDebug(LOG_TAG, TITLE + " socket server successfully started");
            }
        } else {
            Logger.logDebug(LOG_TAG, "Not starting " + TITLE + " socket server since its not enabled");
        }

        // Once nasux-app has started, the server state must not be changed since the variable is
        // exported in shell sessions and tasks and if state is changed, then env of older shells will
        // retain invalid value. User should force stop the app to update state after changing prop.
        NASUX_APP_AM_SOCKET_SERVER_ENABLED = enabled;
        NasUXAppShellEnvironment.updateNasUXAppAMSocketServerEnabled(context);
    }

    /**
     * Create the {@link AmSocketServer} {@link LocalServerSocket} and start listening for new {@link LocalClientSocket}.
     */
    public static synchronized void start(@NonNull Context context) {
        stop();

        AmSocketServerRunConfig amSocketServerRunConfig = new AmSocketServerRunConfig(TITLE,
            NasUXConstants.NASUX_APP.NASUX_AM_SOCKET_FILE_PATH, new NasUXAmSocketServerClient());

        nasuxAmSocketServer = AmSocketServer.start(context, amSocketServerRunConfig);
    }

    /**
     * Stop the {@link AmSocketServer} {@link LocalServerSocket} and stop listening for new {@link LocalClientSocket}.
     */
    public static synchronized void stop() {
        if (nasuxAmSocketServer != null) {
            Error error = nasuxAmSocketServer.stop();
            if (error != null) {
                nasuxAmSocketServer.onError(error);
            }
            nasuxAmSocketServer = null;
        }
    }

    /**
     * Update the state of the {@link AmSocketServer} {@link LocalServerSocket} depending on current
     * value of {@link NasUXPropertyConstants#KEY_RUN_NASUX_AM_SOCKET_SERVER}.
     */
    public static synchronized void updateState(@NonNull Context context) {
        NasUXAppSharedProperties properties = NasUXAppSharedProperties.getProperties();
        if (properties.shouldRunNasUXAmSocketServer()) {
            if (nasuxAmSocketServer == null) {
                Logger.logDebug(LOG_TAG, "updateState: Starting " + TITLE + " socket server");
                start(context);
            }
        } else {
            if (nasuxAmSocketServer != null) {
                Logger.logDebug(LOG_TAG, "updateState: Disabling " + TITLE + " socket server");
                stop();
            }
        }
    }

    /**
     * Get {@link #nasuxAmSocketServer}.
     */
    public static synchronized LocalSocketManager getNasUXAmSocketServer() {
        return nasuxAmSocketServer;
    }

    /**
     * Show an error notification on the {@link NasUXConstants#NASUX_PLUGIN_COMMAND_ERRORS_NOTIFICATION_CHANNEL_ID}
     * {@link NasUXConstants#NASUX_PLUGIN_COMMAND_ERRORS_NOTIFICATION_CHANNEL_NAME} with a call
     * to {@link NasUXPluginUtils#sendPluginCommandErrorNotification(Context, String, CharSequence, String, String)}.
     *
     * @param context The {@link Context} to send the notification with.
     * @param error The {@link Error} generated.
     * @param localSocketRunConfig The {@link LocalSocketRunConfig} for {@link LocalSocketManager}.
     * @param clientSocket The optional {@link LocalClientSocket} for which the error was generated.
     */
    public static synchronized void showErrorNotification(@NonNull Context context, @NonNull Error error,
                                                          @NonNull LocalSocketRunConfig localSocketRunConfig,
                                                          @Nullable LocalClientSocket clientSocket) {
        NasUXPluginUtils.sendPluginCommandErrorNotification(context, LOG_TAG,
            localSocketRunConfig.getTitle() + " Socket Server Error", error.getMinimalErrorString(),
            LocalSocketManager.getErrorMarkdownString(error, localSocketRunConfig, clientSocket));
    }



    public static Boolean getNasUXAppAMSocketServerEnabled(@NonNull Context currentPackageContext) {
        boolean isNasUXApp = NasUXConstants.NASUX_PACKAGE_NAME.equals(currentPackageContext.getPackageName());
        if (isNasUXApp) {
            return NASUX_APP_AM_SOCKET_SERVER_ENABLED;
        } else {
            // Currently, unsupported since plugin app processes don't know that value is set in nasux
            // app process NasUXAmSocketServer class. A binder API or a way to check if server is actually
            // running needs to be used. Long checks would also not be possible on main application thread
            return null;
        }

    }





    /** Enhanced implementation for {@link AmSocketServer.AmSocketServerClient} for {@link NasUXAmSocketServer}. */
    public static class NasUXAmSocketServerClient extends AmSocketServer.AmSocketServerClient {

        public static final String LOG_TAG = "NasUXAmSocketServerClient";

        @Nullable
        @Override
        public Thread.UncaughtExceptionHandler getLocalSocketManagerClientThreadUEH(
            @NonNull LocalSocketManager localSocketManager) {
            // Use nasux crash handler for socket listener thread just like used for main app process thread.
            return NasUXCrashUtils.getCrashHandler(localSocketManager.getContext());
        }

        @Override
        public void onError(@NonNull LocalSocketManager localSocketManager,
                            @Nullable LocalClientSocket clientSocket, @NonNull Error error) {
            // Don't show notification if server is not running since errors may be triggered
            // when server is stopped and server and client sockets are closed.
            if (localSocketManager.isRunning()) {
                NasUXAmSocketServer.showErrorNotification(localSocketManager.getContext(), error,
                    localSocketManager.getLocalSocketRunConfig(), clientSocket);
            }

            // But log the exception
            super.onError(localSocketManager, clientSocket, error);
        }

        @Override
        public void onDisallowedClientConnected(@NonNull LocalSocketManager localSocketManager,
                                                @NonNull LocalClientSocket clientSocket, @NonNull Error error) {
            // Always show notification and log error regardless of if server is running or not
            NasUXAmSocketServer.showErrorNotification(localSocketManager.getContext(), error,
                localSocketManager.getLocalSocketRunConfig(), clientSocket);
            super.onDisallowedClientConnected(localSocketManager, clientSocket, error);
        }



        @Override
        protected String getLogTag() {
            return LOG_TAG;
        }

    }

}
