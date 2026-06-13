package com.nastech.nasux.app.terminal;

import android.app.Service;

import androidx.annotation.NonNull;

import com.nastech.nasux.app.NasUXService;
import com.nastech.nasux.shared.nasux.shell.command.runner.terminal.NasUXSession;
import com.nastech.nasux.shared.nasux.terminal.NasUXTerminalSessionClientBase;
import com.nastech.nasux.terminal.TerminalSession;
import com.nastech.nasux.terminal.TerminalSessionClient;

/** The {@link TerminalSessionClient} implementation that may require a {@link Service} for its interface methods. */
public class NasUXTerminalSessionServiceClient extends NasUXTerminalSessionClientBase {

    private static final String LOG_TAG = "NasUXTerminalSessionServiceClient";

    private final NasUXService mService;

    public NasUXTerminalSessionServiceClient(NasUXService service) {
        this.mService = service;
    }

    @Override
    public void setTerminalShellPid(@NonNull TerminalSession terminalSession, int pid) {
        NasUXSession nasuxSession = mService.getNasUXSessionForTerminalSession(terminalSession);
        if (nasuxSession != null)
            nasuxSession.getExecutionCommand().mPid = pid;
    }

}
