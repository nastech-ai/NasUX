package com.nastech.nasux.app;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nastech.nasux.R;
import com.nastech.nasux.app.event.SystemEventReceiver;
import com.nastech.nasux.app.terminal.NasUXTerminalSessionActivityClient;
import com.nastech.nasux.app.terminal.NasUXTerminalSessionServiceClient;
import com.nastech.nasux.shared.nasux.plugins.NasUXPluginUtils;
import com.nastech.nasux.shared.data.IntentUtils;
import com.nastech.nasux.shared.net.uri.UriUtils;
import com.nastech.nasux.shared.errors.Errno;
import com.nastech.nasux.shared.shell.ShellUtils;
import com.nastech.nasux.shared.shell.command.runner.app.AppShell;
import com.nastech.nasux.shared.nasux.settings.properties.NasUXAppSharedProperties;
import com.nastech.nasux.shared.nasux.shell.command.environment.NasUXShellEnvironment;
import com.nastech.nasux.shared.nasux.shell.NasUXShellUtils;
import com.nastech.nasux.shared.nasux.NasUXConstants;
import com.nastech.nasux.shared.nasux.NasUXConstants.NASUX_APP.NASUX_ACTIVITY;
import com.nastech.nasux.shared.nasux.NasUXConstants.NASUX_APP.NASUX_SERVICE;
import com.nastech.nasux.shared.nasux.settings.preferences.NasUXAppSharedPreferences;
import com.nastech.nasux.shared.nasux.shell.NasUXShellManager;
import com.nastech.nasux.shared.nasux.shell.command.runner.terminal.NasUXSession;
import com.nastech.nasux.shared.nasux.terminal.NasUXTerminalSessionClientBase;
import com.nastech.nasux.shared.logger.Logger;
import com.nastech.nasux.shared.notification.NotificationUtils;
import com.nastech.nasux.shared.android.PermissionUtils;
import com.nastech.nasux.shared.data.DataUtils;
import com.nastech.nasux.shared.shell.command.ExecutionCommand;
import com.nastech.nasux.shared.shell.command.ExecutionCommand.Runner;
import com.nastech.nasux.shared.shell.command.ExecutionCommand.ShellCreateMode;
import com.nastech.nasux.terminal.TerminalEmulator;
import com.nastech.nasux.terminal.TerminalSession;
import com.nastech.nasux.terminal.TerminalSessionClient;

import java.util.ArrayList;
import java.util.List;

/**
 * A service holding a list of {@link NasUXSession} in {@link NasUXShellManager#mNasUXSessions} and background {@link AppShell}
 * in {@link NasUXShellManager#mNasUXTasks}, showing a foreground notification while running so that it is not terminated.
 * The user interacts with the session through {@link NasUXActivity}, but this service may outlive
 * the activity when the user or the system disposes of the activity. In that case the user may
 * restart {@link NasUXActivity} later to yet again access the sessions.
 * <p/>
 * In order to keep both terminal sessions and spawned processes (who may outlive the terminal sessions) alive as long
 * as wanted by the user this service is a foreground service, {@link Service#startForeground(int, Notification)}.
 * <p/>
 * Optionally may hold a wake and a wifi lock, in which case that is shown in the notification - see
 * {@link #buildNotification()}.
 */
public final class NasUXService extends Service implements AppShell.AppShellClient, NasUXSession.NasUXSessionClient {

    /** This service is only bound from inside the same process and never uses IPC. */
    class LocalBinder extends Binder {
        public final NasUXService service = NasUXService.this;
    }

    private final IBinder mBinder = new LocalBinder();

    private final Handler mHandler = new Handler();


    /** The full implementation of the {@link TerminalSessionClient} interface to be used by {@link TerminalSession}
     * that holds activity references for activity related functions.
     * Note that the service may often outlive the activity, so need to clear this reference.
     */
    private NasUXTerminalSessionActivityClient mNasUXTerminalSessionActivityClient;

    /** The basic implementation of the {@link TerminalSessionClient} interface to be used by {@link TerminalSession}
     * that does not hold activity references and only a service reference.
     */
    private final NasUXTerminalSessionServiceClient mNasUXTerminalSessionServiceClient = new NasUXTerminalSessionServiceClient(this);

    /**
     * NasUX app shared properties manager, loaded from nasux.properties
     */
    private NasUXAppSharedProperties mProperties;

    /**
     * NasUX app shell manager
     */
    private NasUXShellManager mShellManager;

    /** The wake lock and wifi lock are always acquired and released together. */
    private PowerManager.WakeLock mWakeLock;
    private WifiManager.WifiLock mWifiLock;

    /** If the user has executed the {@link NASUX_SERVICE#ACTION_STOP_SERVICE} intent. */
    boolean mWantsToStop = false;

    private static final String LOG_TAG = "NasUXService";

    @Override
    public void onCreate() {
        Logger.logVerbose(LOG_TAG, "onCreate");

        // Get NasUX app SharedProperties without loading from disk since NasUXApplication handles
        // load and NasUXActivity handles reloads
        mProperties = NasUXAppSharedProperties.getProperties();

        mShellManager = NasUXShellManager.getShellManager();

        runStartForeground();

        SystemEventReceiver.registerPackageUpdateEvents(this);
    }

    @SuppressLint("Wakelock")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.logDebug(LOG_TAG, "onStartCommand");

        // Run again in case service is already started and onCreate() is not called
        runStartForeground();

        String action = null;
        if (intent != null) {
            Logger.logVerboseExtended(LOG_TAG, "Intent Received:\n" + IntentUtils.getIntentString(intent));
            action = intent.getAction();
        }

        if (action != null) {
            switch (action) {
                case NASUX_SERVICE.ACTION_STOP_SERVICE:
                    Logger.logDebug(LOG_TAG, "ACTION_STOP_SERVICE intent received");
                    actionStopService();
                    break;
                case NASUX_SERVICE.ACTION_WAKE_LOCK:
                    Logger.logDebug(LOG_TAG, "ACTION_WAKE_LOCK intent received");
                    actionAcquireWakeLock();
                    break;
                case NASUX_SERVICE.ACTION_WAKE_UNLOCK:
                    Logger.logDebug(LOG_TAG, "ACTION_WAKE_UNLOCK intent received");
                    actionReleaseWakeLock(true);
                    break;
                case NASUX_SERVICE.ACTION_SERVICE_EXECUTE:
                    Logger.logDebug(LOG_TAG, "ACTION_SERVICE_EXECUTE intent received");
                    actionServiceExecute(intent);
                    break;
                default:
                    Logger.logError(LOG_TAG, "Invalid action: \"" + action + "\"");
                    break;
            }
        }

        // If this service really do get killed, there is no point restarting it automatically - let the user do on next
        // start of {@link Term):
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Logger.logVerbose(LOG_TAG, "onDestroy");

        NasUXShellUtils.clearNasUXTMPDIR(true);

        actionReleaseWakeLock(false);
        if (!mWantsToStop)
            killAllNasUXExecutionCommands();

        NasUXShellManager.onAppExit(this);

        SystemEventReceiver.unregisterPackageUpdateEvents(this);

        runStopForeground();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Logger.logVerbose(LOG_TAG, "onBind");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Logger.logVerbose(LOG_TAG, "onUnbind");

        // Since we cannot rely on {@link NasUXActivity.onDestroy()} to always complete,
        // we unset clients here as well if it failed, so that we do not leave service and session
        // clients with references to the activity.
        if (mNasUXTerminalSessionActivityClient != null)
            unsetNasUXTerminalSessionClient();
        return false;
    }

    /** Make service run in foreground mode. */
    private void runStartForeground() {
        setupNotificationChannel();
        startForeground(NasUXConstants.NASUX_APP_NOTIFICATION_ID, buildNotification());
    }

    /** Make service leave foreground mode. */
    private void runStopForeground() {
        stopForeground(true);
    }

    /** Request to stop service. */
    private void requestStopService() {
        Logger.logDebug(LOG_TAG, "Requesting to stop service");
        runStopForeground();
        stopSelf();
    }

    /** Process action to stop service. */
    private void actionStopService() {
        mWantsToStop = true;
        killAllNasUXExecutionCommands();
        requestStopService();
    }

    /** Kill all NasUXSessions and NasUXTasks by sending SIGKILL to their processes.
     *
     * For NasUXSessions, all sessions will be killed, whether user manually exited NasUX or if
     * onDestroy() was directly called because of unintended shutdown. The processing of results
     * will only be done if user manually exited nasux or if the session was started by a plugin
     * which **expects** the result back via a pending intent.
     *
     * For NasUXTasks, only tasks that were started by a plugin which **expects** the result
     * back via a pending intent will be killed, whether user manually exited NasUX or if
     * onDestroy() was directly called because of unintended shutdown. The processing of results
     * will always be done for the tasks that are killed. The remaining processes will keep on
     * running until the nasux app process is killed by android, like by OOM, so we let them run
     * as long as they can.
     *
     * Some plugin execution commands may not have been processed and added to mNasUXSessions and
     * mNasUXTasks lists before the service is killed, so we maintain a separate
     * mPendingPluginExecutionCommands list for those, so that we can notify the pending intent
     * creators that execution was cancelled.
     *
     * Note that if user didn't manually exit NasUX and if onDestroy() was directly called because
     * of unintended shutdown, like android deciding to kill the service, then there will be no
     * guarantee that onDestroy() will be allowed to finish and nasux app process may be killed before
     * it has finished. This means that in those cases some results may not be sent back to their
     * creators for plugin commands but we still try to process whatever results can be processed
     * despite the unreliable behaviour of onDestroy().
     *
     * Note that if don't kill the processes started by plugins which **expect** the result back
     * and notify their creators that they have been killed, then they may get stuck waiting for
     * the results forever like in case of commands started by NasUX:Tasker or RUN_COMMAND intent,
     * since once NasUXService has been killed, no result will be sent back. They may still get
     * stuck if nasux app process gets killed, so for this case reasonable timeout values should
     * be used, like in Tasker for the NasUX:Tasker actions.
     *
     * We make copies of each list since items are removed inside the loop.
     */
    private synchronized void killAllNasUXExecutionCommands() {
        boolean processResult;

        Logger.logDebug(LOG_TAG, "Killing NasUXSessions=" + mShellManager.mNasUXSessions.size() +
            ", NasUXTasks=" + mShellManager.mNasUXTasks.size() +
            ", PendingPluginExecutionCommands=" + mShellManager.mPendingPluginExecutionCommands.size());

        List<NasUXSession> nasuxSessions = new ArrayList<>(mShellManager.mNasUXSessions);
        List<AppShell> nasuxTasks = new ArrayList<>(mShellManager.mNasUXTasks);
        List<ExecutionCommand> pendingPluginExecutionCommands = new ArrayList<>(mShellManager.mPendingPluginExecutionCommands);

        for (int i = 0; i < nasuxSessions.size(); i++) {
            ExecutionCommand executionCommand = nasuxSessions.get(i).getExecutionCommand();
            processResult = mWantsToStop || executionCommand.isPluginExecutionCommandWithPendingResult();
            nasuxSessions.get(i).killIfExecuting(this, processResult);
            if (!processResult)
                mShellManager.mNasUXSessions.remove(nasuxSessions.get(i));
        }


        for (int i = 0; i < nasuxTasks.size(); i++) {
            ExecutionCommand executionCommand = nasuxTasks.get(i).getExecutionCommand();
            if (executionCommand.isPluginExecutionCommandWithPendingResult())
                nasuxTasks.get(i).killIfExecuting(this, true);
            else
                mShellManager.mNasUXTasks.remove(nasuxTasks.get(i));
        }

        for (int i = 0; i < pendingPluginExecutionCommands.size(); i++) {
            ExecutionCommand executionCommand = pendingPluginExecutionCommands.get(i);
            if (!executionCommand.shouldNotProcessResults() && executionCommand.isPluginExecutionCommandWithPendingResult()) {
                if (executionCommand.setStateFailed(Errno.ERRNO_CANCELLED.getCode(), this.getString(com.nastech.nasux.shared.R.string.error_execution_cancelled))) {
                    NasUXPluginUtils.processPluginExecutionCommandResult(this, LOG_TAG, executionCommand);
                }
            }
        }
    }



    /** Process action to acquire Power and Wi-Fi WakeLocks. */
    @SuppressLint({"WakelockTimeout", "BatteryLife"})
    private void actionAcquireWakeLock() {
        if (mWakeLock != null) {
            Logger.logDebug(LOG_TAG, "Ignoring acquiring WakeLocks since they are already held");
            return;
        }

        Logger.logDebug(LOG_TAG, "Acquiring WakeLocks");

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, NasUXConstants.NASUX_APP_NAME.toLowerCase() + ":service-wakelock");
        mWakeLock.acquire();

        // http://tools.android.com/tech-docs/lint-in-studio-2-3#TOC-WifiManager-Leak
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mWifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, NasUXConstants.NASUX_APP_NAME.toLowerCase());
        mWifiLock.acquire();

        if (!PermissionUtils.checkIfBatteryOptimizationsDisabled(this)) {
            PermissionUtils.requestDisableBatteryOptimizations(this);
        }

        updateNotification();

        Logger.logDebug(LOG_TAG, "WakeLocks acquired successfully");

    }

    /** Process action to release Power and Wi-Fi WakeLocks. */
    private void actionReleaseWakeLock(boolean updateNotification) {
        if (mWakeLock == null && mWifiLock == null) {
            Logger.logDebug(LOG_TAG, "Ignoring releasing WakeLocks since none are already held");
            return;
        }

        Logger.logDebug(LOG_TAG, "Releasing WakeLocks");

        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }

        if (mWifiLock != null) {
            mWifiLock.release();
            mWifiLock = null;
        }

        if (updateNotification)
            updateNotification();

        Logger.logDebug(LOG_TAG, "WakeLocks released successfully");
    }

    /** Process {@link NASUX_SERVICE#ACTION_SERVICE_EXECUTE} intent to execute a shell command in
     * a foreground NasUXSession or in a background NasUXTask. */
    private void actionServiceExecute(Intent intent) {
        if (intent == null) {
            Logger.logError(LOG_TAG, "Ignoring null intent to actionServiceExecute");
            return;
        }

        ExecutionCommand executionCommand = new ExecutionCommand(NasUXShellManager.getNextShellId());

        executionCommand.executableUri = intent.getData();
        executionCommand.isPluginExecutionCommand = true;

        // If EXTRA_RUNNER is passed, use that, otherwise check EXTRA_BACKGROUND and default to Runner.TERMINAL_SESSION
        executionCommand.runner = IntentUtils.getStringExtraIfSet(intent, NASUX_SERVICE.EXTRA_RUNNER,
            (intent.getBooleanExtra(NASUX_SERVICE.EXTRA_BACKGROUND, false) ? Runner.APP_SHELL.getName() : Runner.TERMINAL_SESSION.getName()));
        if (Runner.runnerOf(executionCommand.runner) == null) {
            String errmsg = this.getString(R.string.error_nasux_service_invalid_execution_command_runner, executionCommand.runner);
            executionCommand.setStateFailed(Errno.ERRNO_FAILED.getCode(), errmsg);
            NasUXPluginUtils.processPluginExecutionCommandError(this, LOG_TAG, executionCommand, false);
            return;
        }

        if (executionCommand.executableUri != null) {
            Logger.logVerbose(LOG_TAG, "uri: \"" + executionCommand.executableUri + "\", path: \"" + executionCommand.executableUri.getPath() + "\", fragment: \"" + executionCommand.executableUri.getFragment() + "\"");

            // Get full path including fragment (anything after last "#")
            executionCommand.executable = UriUtils.getUriFilePathWithFragment(executionCommand.executableUri);
            executionCommand.arguments = IntentUtils.getStringArrayExtraIfSet(intent, NASUX_SERVICE.EXTRA_ARGUMENTS, null);
            if (Runner.APP_SHELL.equalsRunner(executionCommand.runner))
                executionCommand.stdin = IntentUtils.getStringExtraIfSet(intent, NASUX_SERVICE.EXTRA_STDIN, null);
            executionCommand.backgroundCustomLogLevel = IntentUtils.getIntegerExtraIfSet(intent, NASUX_SERVICE.EXTRA_BACKGROUND_CUSTOM_LOG_LEVEL, null);
        }

        executionCommand.workingDirectory = IntentUtils.getStringExtraIfSet(intent, NASUX_SERVICE.EXTRA_WORKDIR, null);
        executionCommand.isFailsafe = intent.getBooleanExtra(NASUX_ACTIVITY.EXTRA_FAILSAFE_SESSION, false);
        executionCommand.sessionAction = intent.getStringExtra(NASUX_SERVICE.EXTRA_SESSION_ACTION);
        executionCommand.shellName = IntentUtils.getStringExtraIfSet(intent, NASUX_SERVICE.EXTRA_SHELL_NAME, null);
        executionCommand.shellCreateMode = IntentUtils.getStringExtraIfSet(intent, NASUX_SERVICE.EXTRA_SHELL_CREATE_MODE, null);
        executionCommand.commandLabel = IntentUtils.getStringExtraIfSet(intent, NASUX_SERVICE.EXTRA_COMMAND_LABEL, "Execution Intent Command");
        executionCommand.commandDescription = IntentUtils.getStringExtraIfSet(intent, NASUX_SERVICE.EXTRA_COMMAND_DESCRIPTION, null);
        executionCommand.commandHelp = IntentUtils.getStringExtraIfSet(intent, NASUX_SERVICE.EXTRA_COMMAND_HELP, null);
        executionCommand.pluginAPIHelp = IntentUtils.getStringExtraIfSet(intent, NASUX_SERVICE.EXTRA_PLUGIN_API_HELP, null);
        executionCommand.resultConfig.resultPendingIntent = intent.getParcelableExtra(NASUX_SERVICE.EXTRA_PENDING_INTENT);
        executionCommand.resultConfig.resultDirectoryPath = IntentUtils.getStringExtraIfSet(intent, NASUX_SERVICE.EXTRA_RESULT_DIRECTORY, null);
        if (executionCommand.resultConfig.resultDirectoryPath != null) {
            executionCommand.resultConfig.resultSingleFile = intent.getBooleanExtra(NASUX_SERVICE.EXTRA_RESULT_SINGLE_FILE, false);
            executionCommand.resultConfig.resultFileBasename = IntentUtils.getStringExtraIfSet(intent, NASUX_SERVICE.EXTRA_RESULT_FILE_BASENAME, null);
            executionCommand.resultConfig.resultFileOutputFormat = IntentUtils.getStringExtraIfSet(intent, NASUX_SERVICE.EXTRA_RESULT_FILE_OUTPUT_FORMAT, null);
            executionCommand.resultConfig.resultFileErrorFormat = IntentUtils.getStringExtraIfSet(intent, NASUX_SERVICE.EXTRA_RESULT_FILE_ERROR_FORMAT, null);
            executionCommand.resultConfig.resultFilesSuffix = IntentUtils.getStringExtraIfSet(intent, NASUX_SERVICE.EXTRA_RESULT_FILES_SUFFIX, null);
        }

        if (executionCommand.shellCreateMode == null)
            executionCommand.shellCreateMode = ShellCreateMode.ALWAYS.getMode();

        // Add the execution command to pending plugin execution commands list
        mShellManager.mPendingPluginExecutionCommands.add(executionCommand);

        if (Runner.APP_SHELL.equalsRunner(executionCommand.runner))
            executeNasUXTaskCommand(executionCommand);
        else if (Runner.TERMINAL_SESSION.equalsRunner(executionCommand.runner))
            executeNasUXSessionCommand(executionCommand);
        else {
            String errmsg = getString(R.string.error_nasux_service_unsupported_execution_command_runner, executionCommand.runner);
            executionCommand.setStateFailed(Errno.ERRNO_FAILED.getCode(), errmsg);
            NasUXPluginUtils.processPluginExecutionCommandError(this, LOG_TAG, executionCommand, false);
        }
    }





    /** Execute a shell command in background NasUXTask. */
    private void executeNasUXTaskCommand(ExecutionCommand executionCommand) {
        if (executionCommand == null) return;

        Logger.logDebug(LOG_TAG, "Executing background \"" + executionCommand.getCommandIdAndLabelLogString() + "\" NasUXTask command");

        // Transform executable path to shell/session name, e.g. "/bin/do-something.sh" => "do-something.sh".
        if (executionCommand.shellName == null && executionCommand.executable != null)
            executionCommand.shellName = ShellUtils.getExecutableBasename(executionCommand.executable);

        AppShell newNasUXTask = null;
        ShellCreateMode shellCreateMode = processShellCreateMode(executionCommand);
        if (shellCreateMode == null) return;
        if (ShellCreateMode.NO_SHELL_WITH_NAME.equals(shellCreateMode)) {
            newNasUXTask = getNasUXTaskForShellName(executionCommand.shellName);
            if (newNasUXTask != null)
                Logger.logVerbose(LOG_TAG, "Existing NasUXTask with \"" + executionCommand.shellName + "\" shell name found for shell create mode \"" + shellCreateMode.getMode() + "\"");
            else
                Logger.logVerbose(LOG_TAG, "No existing NasUXTask with \"" + executionCommand.shellName + "\" shell name found for shell create mode \"" + shellCreateMode.getMode() + "\"");
        }

        if (newNasUXTask == null)
            newNasUXTask = createNasUXTask(executionCommand);
    }

    /** Create a NasUXTask. */
    @Nullable
    public AppShell createNasUXTask(String executablePath, String[] arguments, String stdin, String workingDirectory) {
        return createNasUXTask(new ExecutionCommand(NasUXShellManager.getNextShellId(), executablePath,
            arguments, stdin, workingDirectory, Runner.APP_SHELL.getName(), false));
    }

    /** Create a NasUXTask. */
    @Nullable
    public synchronized AppShell createNasUXTask(ExecutionCommand executionCommand) {
        if (executionCommand == null) return null;

        Logger.logDebug(LOG_TAG, "Creating \"" + executionCommand.getCommandIdAndLabelLogString() + "\" NasUXTask");

        if (!Runner.APP_SHELL.equalsRunner(executionCommand.runner)) {
            Logger.logDebug(LOG_TAG, "Ignoring wrong runner \"" + executionCommand.runner + "\" command passed to createNasUXTask()");
            return null;
        }

        executionCommand.setShellCommandShellEnvironment = true;

        if (Logger.getLogLevel() >= Logger.LOG_LEVEL_VERBOSE)
            Logger.logVerboseExtended(LOG_TAG, executionCommand.toString());

        AppShell newNasUXTask = AppShell.execute(this, executionCommand, this,
            new NasUXShellEnvironment(), null,false);
        if (newNasUXTask == null) {
            Logger.logError(LOG_TAG, "Failed to execute new NasUXTask command for:\n" + executionCommand.getCommandIdAndLabelLogString());
            // If the execution command was started for a plugin, then process the error
            if (executionCommand.isPluginExecutionCommand)
                NasUXPluginUtils.processPluginExecutionCommandError(this, LOG_TAG, executionCommand, false);
            else {
                Logger.logError(LOG_TAG, "Set log level to debug or higher to see error in logs");
                Logger.logErrorPrivateExtended(LOG_TAG, executionCommand.toString());
            }
            return null;
        }

        mShellManager.mNasUXTasks.add(newNasUXTask);

        // Remove the execution command from the pending plugin execution commands list since it has
        // now been processed
        if (executionCommand.isPluginExecutionCommand)
            mShellManager.mPendingPluginExecutionCommands.remove(executionCommand);

        updateNotification();

        return newNasUXTask;
    }

    /** Callback received when a NasUXTask finishes. */
    @Override
    public void onAppShellExited(final AppShell nasuxTask) {
        mHandler.post(() -> {
            if (nasuxTask != null) {
                ExecutionCommand executionCommand = nasuxTask.getExecutionCommand();

                Logger.logVerbose(LOG_TAG, "The onNasUXTaskExited() callback called for \"" + executionCommand.getCommandIdAndLabelLogString() + "\" NasUXTask command");

                // If the execution command was started for a plugin, then process the results
                if (executionCommand != null && executionCommand.isPluginExecutionCommand)
                    NasUXPluginUtils.processPluginExecutionCommandResult(this, LOG_TAG, executionCommand);

                mShellManager.mNasUXTasks.remove(nasuxTask);
            }

            updateNotification();
        });
    }





    /** Execute a shell command in a foreground {@link NasUXSession}. */
    private void executeNasUXSessionCommand(ExecutionCommand executionCommand) {
        if (executionCommand == null) return;

        Logger.logDebug(LOG_TAG, "Executing foreground \"" + executionCommand.getCommandIdAndLabelLogString() + "\" NasUXSession command");

        // Transform executable path to shell/session name, e.g. "/bin/do-something.sh" => "do-something.sh".
        if (executionCommand.shellName == null && executionCommand.executable != null)
            executionCommand.shellName = ShellUtils.getExecutableBasename(executionCommand.executable);

        NasUXSession newNasUXSession = null;
        ShellCreateMode shellCreateMode = processShellCreateMode(executionCommand);
        if (shellCreateMode == null) return;
        if (ShellCreateMode.NO_SHELL_WITH_NAME.equals(shellCreateMode)) {
            newNasUXSession = getNasUXSessionForShellName(executionCommand.shellName);
            if (newNasUXSession != null)
                Logger.logVerbose(LOG_TAG, "Existing NasUXSession with \"" + executionCommand.shellName + "\" shell name found for shell create mode \"" + shellCreateMode.getMode() + "\"");
            else
                Logger.logVerbose(LOG_TAG, "No existing NasUXSession with \"" + executionCommand.shellName + "\" shell name found for shell create mode \"" + shellCreateMode.getMode() + "\"");
        }

        if (newNasUXSession == null)
            newNasUXSession = createNasUXSession(executionCommand);
        if (newNasUXSession == null) return;

        handleSessionAction(DataUtils.getIntFromString(executionCommand.sessionAction,
            NASUX_SERVICE.VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_OPEN_ACTIVITY),
            newNasUXSession.getTerminalSession());
    }

    /**
     * Create a {@link NasUXSession}.
     * Currently called by {@link NasUXTerminalSessionActivityClient#addNewSession(boolean, String)} to add a new {@link NasUXSession}.
     */
    @Nullable
    public NasUXSession createNasUXSession(String executablePath, String[] arguments, String stdin,
                                             String workingDirectory, boolean isFailSafe, String sessionName) {
        ExecutionCommand executionCommand = new ExecutionCommand(NasUXShellManager.getNextShellId(),
            executablePath, arguments, stdin, workingDirectory, Runner.TERMINAL_SESSION.getName(), isFailSafe);
        executionCommand.shellName = sessionName;
        return createNasUXSession(executionCommand);
    }

    /** Create a {@link NasUXSession}. */
    @Nullable
    public synchronized NasUXSession createNasUXSession(ExecutionCommand executionCommand) {
        if (executionCommand == null) return null;

        Logger.logDebug(LOG_TAG, "Creating \"" + executionCommand.getCommandIdAndLabelLogString() + "\" NasUXSession");

        if (!Runner.TERMINAL_SESSION.equalsRunner(executionCommand.runner)) {
            Logger.logDebug(LOG_TAG, "Ignoring wrong runner \"" + executionCommand.runner + "\" command passed to createNasUXSession()");
            return null;
        }

        executionCommand.setShellCommandShellEnvironment = true;
        executionCommand.terminalTranscriptRows = mProperties.getTerminalTranscriptRows();

        if (Logger.getLogLevel() >= Logger.LOG_LEVEL_VERBOSE)
            Logger.logVerboseExtended(LOG_TAG, executionCommand.toString());

        // If the execution command was started for a plugin, only then will the stdout be set
        // Otherwise if command was manually started by the user like by adding a new terminal session,
        // then no need to set stdout
        NasUXSession newNasUXSession = NasUXSession.execute(this, executionCommand, getNasUXTerminalSessionClient(),
            this, new NasUXShellEnvironment(), null, executionCommand.isPluginExecutionCommand);
        if (newNasUXSession == null) {
            Logger.logError(LOG_TAG, "Failed to execute new NasUXSession command for:\n" + executionCommand.getCommandIdAndLabelLogString());
            // If the execution command was started for a plugin, then process the error
            if (executionCommand.isPluginExecutionCommand)
                NasUXPluginUtils.processPluginExecutionCommandError(this, LOG_TAG, executionCommand, false);
            else {
                Logger.logError(LOG_TAG, "Set log level to debug or higher to see error in logs");
                Logger.logErrorPrivateExtended(LOG_TAG, executionCommand.toString());
            }
            return null;
        }

        mShellManager.mNasUXSessions.add(newNasUXSession);

        // Remove the execution command from the pending plugin execution commands list since it has
        // now been processed
        if (executionCommand.isPluginExecutionCommand)
            mShellManager.mPendingPluginExecutionCommands.remove(executionCommand);

        // Notify {@link NasUXSessionsListViewController} that sessions list has been updated if
        // activity in is foreground
        if (mNasUXTerminalSessionActivityClient != null)
            mNasUXTerminalSessionActivityClient.nasuxSessionListNotifyUpdated();

        updateNotification();

        // No need to recreate the activity since it likely just started and theme should already have applied
        NasUXActivity.updateNasUXActivityStyling(this, false);

        return newNasUXSession;
    }

    /** Remove a NasUXSession. */
    public synchronized int removeNasUXSession(TerminalSession sessionToRemove) {
        int index = getIndexOfSession(sessionToRemove);

        if (index >= 0)
            mShellManager.mNasUXSessions.get(index).finish();

        return index;
    }

    /** Callback received when a {@link NasUXSession} finishes. */
    @Override
    public void onNasUXSessionExited(final NasUXSession nasuxSession) {
        if (nasuxSession != null) {
            ExecutionCommand executionCommand = nasuxSession.getExecutionCommand();

            Logger.logVerbose(LOG_TAG, "The onNasUXSessionExited() callback called for \"" + executionCommand.getCommandIdAndLabelLogString() + "\" NasUXSession command");

            // If the execution command was started for a plugin, then process the results
            if (executionCommand != null && executionCommand.isPluginExecutionCommand)
                NasUXPluginUtils.processPluginExecutionCommandResult(this, LOG_TAG, executionCommand);

            mShellManager.mNasUXSessions.remove(nasuxSession);

            // Notify {@link NasUXSessionsListViewController} that sessions list has been updated if
            // activity in is foreground
            if (mNasUXTerminalSessionActivityClient != null)
                mNasUXTerminalSessionActivityClient.nasuxSessionListNotifyUpdated();
        }

        updateNotification();
    }





    private ShellCreateMode processShellCreateMode(@NonNull ExecutionCommand executionCommand) {
        if (ShellCreateMode.ALWAYS.equalsMode(executionCommand.shellCreateMode))
            return ShellCreateMode.ALWAYS; // Default
        else if (ShellCreateMode.NO_SHELL_WITH_NAME.equalsMode(executionCommand.shellCreateMode))
            if (DataUtils.isNullOrEmpty(executionCommand.shellName)) {
                NasUXPluginUtils.setAndProcessPluginExecutionCommandError(this, LOG_TAG, executionCommand, false,
                    getString(R.string.error_nasux_service_execution_command_shell_name_unset, executionCommand.shellCreateMode));
                return null;
            } else {
               return ShellCreateMode.NO_SHELL_WITH_NAME;
            }
        else {
            NasUXPluginUtils.setAndProcessPluginExecutionCommandError(this, LOG_TAG, executionCommand, false,
                getString(R.string.error_nasux_service_unsupported_execution_command_shell_create_mode, executionCommand.shellCreateMode));
            return null;
        }
    }

    /** Process session action for new session. */
    private void handleSessionAction(int sessionAction, TerminalSession newTerminalSession) {
        Logger.logDebug(LOG_TAG, "Processing sessionAction \"" + sessionAction + "\" for session \"" + newTerminalSession.mSessionName + "\"");

        switch (sessionAction) {
            case NASUX_SERVICE.VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_OPEN_ACTIVITY:
                setCurrentStoredTerminalSession(newTerminalSession);
                if (mNasUXTerminalSessionActivityClient != null)
                    mNasUXTerminalSessionActivityClient.setCurrentSession(newTerminalSession);
                startNasUXActivity();
                break;
            case NASUX_SERVICE.VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_OPEN_ACTIVITY:
                if (getNasUXSessionsSize() == 1)
                    setCurrentStoredTerminalSession(newTerminalSession);
                startNasUXActivity();
                break;
            case NASUX_SERVICE.VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_DONT_OPEN_ACTIVITY:
                setCurrentStoredTerminalSession(newTerminalSession);
                if (mNasUXTerminalSessionActivityClient != null)
                    mNasUXTerminalSessionActivityClient.setCurrentSession(newTerminalSession);
                break;
            case NASUX_SERVICE.VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_DONT_OPEN_ACTIVITY:
                if (getNasUXSessionsSize() == 1)
                    setCurrentStoredTerminalSession(newTerminalSession);
                break;
            default:
                Logger.logError(LOG_TAG, "Invalid sessionAction: \"" + sessionAction + "\". Force using default sessionAction.");
                handleSessionAction(NASUX_SERVICE.VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_OPEN_ACTIVITY, newTerminalSession);
                break;
        }
    }

    /** Launch the {@link }NasUXActivity} to bring it to foreground. */
    private void startNasUXActivity() {
        // For android >= 10, apps require Display over other apps permission to start foreground activities
        // from background (services). If it is not granted, then NasUXSessions that are started will
        // show in NasUX notification but will not run until user manually clicks the notification.
        if (PermissionUtils.validateDisplayOverOtherAppsPermissionForPostAndroid10(this, true)) {
            NasUXActivity.startNasUXActivity(this);
        } else {
            NasUXAppSharedPreferences preferences = NasUXAppSharedPreferences.build(this);
            if (preferences == null) return;
            if (preferences.arePluginErrorNotificationsEnabled(false))
                Logger.showToast(this, this.getString(R.string.error_display_over_other_apps_permission_not_granted_to_start_terminal), true);
        }
    }





    /** If {@link NasUXActivity} has not bound to the {@link NasUXService} yet or is destroyed, then
     * interface functions requiring the activity should not be available to the terminal sessions,
     * so we just return the {@link #mNasUXTerminalSessionServiceClient}. Once {@link NasUXActivity} bind
     * callback is received, it should call {@link #setNasUXTerminalSessionClient} to set the
     * {@link NasUXService#mNasUXTerminalSessionActivityClient} so that further terminal sessions are directly
     * passed the {@link NasUXTerminalSessionActivityClient} object which fully implements the
     * {@link TerminalSessionClient} interface.
     *
     * @return Returns the {@link NasUXTerminalSessionActivityClient} if {@link NasUXActivity} has bound with
     * {@link NasUXService}, otherwise {@link NasUXTerminalSessionServiceClient}.
     */
    public synchronized NasUXTerminalSessionClientBase getNasUXTerminalSessionClient() {
        if (mNasUXTerminalSessionActivityClient != null)
            return mNasUXTerminalSessionActivityClient;
        else
            return mNasUXTerminalSessionServiceClient;
    }

    /** This should be called when {@link NasUXActivity#onServiceConnected} is called to set the
     * {@link NasUXService#mNasUXTerminalSessionActivityClient} variable and update the {@link TerminalSession}
     * and {@link TerminalEmulator} clients in case they were passed {@link NasUXTerminalSessionServiceClient}
     * earlier.
     *
     * @param nasuxTerminalSessionActivityClient The {@link NasUXTerminalSessionActivityClient} object that fully
     * implements the {@link TerminalSessionClient} interface.
     */
    public synchronized void setNasUXTerminalSessionClient(NasUXTerminalSessionActivityClient nasuxTerminalSessionActivityClient) {
        mNasUXTerminalSessionActivityClient = nasuxTerminalSessionActivityClient;

        for (int i = 0; i < mShellManager.mNasUXSessions.size(); i++)
            mShellManager.mNasUXSessions.get(i).getTerminalSession().updateTerminalSessionClient(mNasUXTerminalSessionActivityClient);
    }

    /** This should be called when {@link NasUXActivity} has been destroyed and in {@link #onUnbind(Intent)}
     * so that the {@link NasUXService} and {@link TerminalSession} and {@link TerminalEmulator}
     * clients do not hold an activity references.
     */
    public synchronized void unsetNasUXTerminalSessionClient() {
        for (int i = 0; i < mShellManager.mNasUXSessions.size(); i++)
            mShellManager.mNasUXSessions.get(i).getTerminalSession().updateTerminalSessionClient(mNasUXTerminalSessionServiceClient);

        mNasUXTerminalSessionActivityClient = null;
    }





    private Notification buildNotification() {
        Resources res = getResources();

        // Set pending intent to be launched when notification is clicked
        Intent notificationIntent = NasUXActivity.newInstance(this);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);


        // Set notification text
        int sessionCount = getNasUXSessionsSize();
        int taskCount = mShellManager.mNasUXTasks.size();
        String notificationText = sessionCount + " session" + (sessionCount == 1 ? "" : "s");
        if (taskCount > 0) {
            notificationText += ", " + taskCount + " task" + (taskCount == 1 ? "" : "s");
        }

        final boolean wakeLockHeld = mWakeLock != null;
        if (wakeLockHeld) notificationText += " (wake lock held)";


        // Set notification priority
        // If holding a wake or wifi lock consider the notification of high priority since it's using power,
        // otherwise use a low priority
        int priority = (wakeLockHeld) ? Notification.PRIORITY_HIGH : Notification.PRIORITY_LOW;


        // Build the notification
        Notification.Builder builder =  NotificationUtils.geNotificationBuilder(this,
            NasUXConstants.NASUX_APP_NOTIFICATION_CHANNEL_ID, priority,
            NasUXConstants.NASUX_APP_NAME, notificationText, null,
            contentIntent, null, NotificationUtils.NOTIFICATION_MODE_SILENT);
        if (builder == null)  return null;

        // No need to show a timestamp:
        builder.setShowWhen(false);

        // Set notification icon
        builder.setSmallIcon(R.drawable.ic_service_notification);

        // Set background color for small notification icon
        builder.setColor(0xFF607D8B);

        // NasUXSessions are always ongoing
        builder.setOngoing(true);


        // Set Exit button action
        Intent exitIntent = new Intent(this, NasUXService.class).setAction(NASUX_SERVICE.ACTION_STOP_SERVICE);
        builder.addAction(android.R.drawable.ic_delete, res.getString(R.string.notification_action_exit), PendingIntent.getService(this, 0, exitIntent, 0));


        // Set Wakelock button actions
        String newWakeAction = wakeLockHeld ? NASUX_SERVICE.ACTION_WAKE_UNLOCK : NASUX_SERVICE.ACTION_WAKE_LOCK;
        Intent toggleWakeLockIntent = new Intent(this, NasUXService.class).setAction(newWakeAction);
        String actionTitle = res.getString(wakeLockHeld ? R.string.notification_action_wake_unlock : R.string.notification_action_wake_lock);
        int actionIcon = wakeLockHeld ? android.R.drawable.ic_lock_idle_lock : android.R.drawable.ic_lock_lock;
        builder.addAction(actionIcon, actionTitle, PendingIntent.getService(this, 0, toggleWakeLockIntent, 0));


        return builder.build();
    }

    private void setupNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationUtils.setupNotificationChannel(this, NasUXConstants.NASUX_APP_NOTIFICATION_CHANNEL_ID,
            NasUXConstants.NASUX_APP_NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
    }

    /** Update the shown foreground service notification after making any changes that affect it. */
    private synchronized void updateNotification() {
        if (mWakeLock == null && mShellManager.mNasUXSessions.isEmpty() && mShellManager.mNasUXTasks.isEmpty()) {
            // Exit if we are updating after the user disabled all locks with no sessions or tasks running.
            requestStopService();
        } else {
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NasUXConstants.NASUX_APP_NOTIFICATION_ID, buildNotification());
        }
    }





    private void setCurrentStoredTerminalSession(TerminalSession terminalSession) {
        if (terminalSession == null) return;
        // Make the newly created session the current one to be displayed
        NasUXAppSharedPreferences preferences = NasUXAppSharedPreferences.build(this);
        if (preferences == null) return;
        preferences.setCurrentSession(terminalSession.mHandle);
    }

    public synchronized boolean isNasUXSessionsEmpty() {
        return mShellManager.mNasUXSessions.isEmpty();
    }

    public synchronized int getNasUXSessionsSize() {
        return mShellManager.mNasUXSessions.size();
    }

    public synchronized List<NasUXSession> getNasUXSessions() {
        return mShellManager.mNasUXSessions;
    }

    @Nullable
    public synchronized NasUXSession getNasUXSession(int index) {
        if (index >= 0 && index < mShellManager.mNasUXSessions.size())
            return mShellManager.mNasUXSessions.get(index);
        else
            return null;
    }

    @Nullable
    public synchronized NasUXSession getNasUXSessionForTerminalSession(TerminalSession terminalSession) {
        if (terminalSession == null) return null;

        for (int i = 0; i < mShellManager.mNasUXSessions.size(); i++) {
            if (mShellManager.mNasUXSessions.get(i).getTerminalSession().equals(terminalSession))
                return mShellManager.mNasUXSessions.get(i);
        }

        return null;
    }

    public synchronized NasUXSession getLastNasUXSession() {
        return mShellManager.mNasUXSessions.isEmpty() ? null : mShellManager.mNasUXSessions.get(mShellManager.mNasUXSessions.size() - 1);
    }

    public synchronized int getIndexOfSession(TerminalSession terminalSession) {
        if (terminalSession == null) return -1;

        for (int i = 0; i < mShellManager.mNasUXSessions.size(); i++) {
            if (mShellManager.mNasUXSessions.get(i).getTerminalSession().equals(terminalSession))
                return i;
        }
        return -1;
    }

    public synchronized TerminalSession getTerminalSessionForHandle(String sessionHandle) {
        TerminalSession terminalSession;
        for (int i = 0, len = mShellManager.mNasUXSessions.size(); i < len; i++) {
            terminalSession = mShellManager.mNasUXSessions.get(i).getTerminalSession();
            if (terminalSession.mHandle.equals(sessionHandle))
                return terminalSession;
        }
        return null;
    }

    public synchronized AppShell getNasUXTaskForShellName(String name) {
        if (DataUtils.isNullOrEmpty(name)) return null;
        AppShell appShell;
        for (int i = 0, len = mShellManager.mNasUXTasks.size(); i < len; i++) {
            appShell = mShellManager.mNasUXTasks.get(i);
            String shellName = appShell.getExecutionCommand().shellName;
            if (shellName != null && shellName.equals(name))
                return appShell;
        }
        return null;
    }

    public synchronized NasUXSession getNasUXSessionForShellName(String name) {
        if (DataUtils.isNullOrEmpty(name)) return null;
        NasUXSession nasuxSession;
        for (int i = 0, len = mShellManager.mNasUXSessions.size(); i < len; i++) {
            nasuxSession = mShellManager.mNasUXSessions.get(i);
            String shellName = nasuxSession.getExecutionCommand().shellName;
            if (shellName != null && shellName.equals(name))
                return nasuxSession;
        }
        return null;
    }



    public boolean wantsToStop() {
        return mWantsToStop;
    }

}
