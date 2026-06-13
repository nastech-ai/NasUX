package com.nastech.nasux.am;

public class ShellCommand {
    private String[] mArgs;
    private int mArgPos;
    private String mCurArgData;

    public void init(String[] args, int firstArgPos) {
        mArgs = args;
        mArgPos = firstArgPos;
        mCurArgData = null;
    }

    public String getNextOption() {
        if (mCurArgData != null) {
            String prev = mArgs[mArgPos - 1];
            throw new IllegalArgumentException("No argument expected after \"" + prev + "\"");
        }
        if (mArgPos >= mArgs.length) {
            return null;
        }
        String arg = mArgs[mArgPos];
        if (!arg.startsWith("-")) {
            return null;
        }
        mArgPos++;
        if (arg.equals("--")) {
            return null;
        }
        if (arg.length() > 1 && arg.charAt(1) != '-') {
            if (arg.length() > 2) {
                mCurArgData = arg.substring(2);
                return arg.substring(0, 2);
            } else {
                mCurArgData = null;
                return arg;
            }
        }
        mCurArgData = null;
        return arg;
    }

    public String getNextArg() {
        if (mCurArgData != null) {
            String arg = mCurArgData;
            mCurArgData = null;
            return arg;
        } else if (mArgPos < mArgs.length) {
            return mArgs[mArgPos++];
        } else {
            return null;
        }
    }

    public String peekNextArg() {
        if (mCurArgData != null) {
            return mCurArgData;
        } else if (mArgPos < mArgs.length) {
            return mArgs[mArgPos];
        } else {
            return null;
        }
    }

    public String getNextArgRequired() {
        String arg = getNextArg();
        if (arg == null) {
            String prev = mArgs[mArgPos - 1];
            throw new IllegalArgumentException("Argument expected after \"" + prev + "\"");
        }
        return arg;
    }
}
