package com.nastech.nasux.am;

import java.io.PrintStream;

public abstract class BaseCommand {

    final protected ShellCommand mArgs = new ShellCommand();

    public static final String FATAL_ERROR_CODE = "Error type 1";
    public static final String NO_SYSTEM_ERROR_CODE = "Error type 2";
    public static final String NO_CLASS_ERROR_CODE = "Error type 3";

    protected PrintStream out;
    protected PrintStream err;

    public BaseCommand(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
    }

    public int run(String[] args) {
        if (args.length < 1) {
            onShowUsage(out);
            return 1;
        }
        mArgs.init(args, 0);
        try {
            onRun();
        } catch (IllegalArgumentException e) {
            onShowUsage(err);
            err.println();
            err.println("Error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace(err);
            return 1;
        }
        return 0;
    }

    public void showUsage() {
        onShowUsage(err);
    }

    public void showError(String message) {
        onShowUsage(err);
        err.println();
        err.println(message);
    }

    public abstract void onRun() throws Exception;

    public abstract void onShowUsage(PrintStream out);

    public String nextOption() {
        return mArgs.getNextOption();
    }

    public String nextArg() {
        return mArgs.getNextArg();
    }

    public String nextArgRequired() {
        return mArgs.getNextArgRequired();
    }
}
