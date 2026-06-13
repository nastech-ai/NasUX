// Source: github.com/termux/termux-am-library v2.0.0 — repackaged for NasUX (com.nastech.nasux.am)

package com.nastech.nasux.am;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Copied from android-7.0.0_r1 frameworks/base/core/java/android/content/Intent.java
 */
public class IntentCmd {

    public interface CommandOptionHandler {
        boolean handleOption(String opt, ShellCommand cmd);
    }

    public static Intent parseCommandArgs(ShellCommand cmd, CommandOptionHandler optionHandler)
            throws URISyntaxException {
        Intent intent = new Intent();
        Intent baseIntent = intent;
        boolean hasIntentInfo = false;

        Uri data = null;
        String type = null;

        String opt;
        while ((opt = cmd.getNextOption()) != null) {
            switch (opt) {
                case "-a":
                    intent.setAction(cmd.getNextArgRequired());
                    if (intent == baseIntent) hasIntentInfo = true;
                    break;
                case "-d":
                    data = Uri.parse(cmd.getNextArgRequired());
                    if (intent == baseIntent) hasIntentInfo = true;
                    break;
                case "-t":
                    type = cmd.getNextArgRequired();
                    if (intent == baseIntent) hasIntentInfo = true;
                    break;
                case "-c":
                    intent.addCategory(cmd.getNextArgRequired());
                    if (intent == baseIntent) hasIntentInfo = true;
                    break;
                case "-e":
                case "--es": {
                    String key = cmd.getNextArgRequired();
                    String value = cmd.getNextArgRequired();
                    intent.putExtra(key, value);
                    break;
                }
                case "--esn": {
                    String key = cmd.getNextArgRequired();
                    intent.putExtra(key, (String) null);
                    break;
                }
                case "--ei": {
                    String key = cmd.getNextArgRequired();
                    String value = cmd.getNextArgRequired();
                    intent.putExtra(key, Integer.decode(value));
                    break;
                }
                case "--eu": {
                    String key = cmd.getNextArgRequired();
                    String value = cmd.getNextArgRequired();
                    intent.putExtra(key, Uri.parse(value));
                    break;
                }
                case "--ecn": {
                    String key = cmd.getNextArgRequired();
                    String value = cmd.getNextArgRequired();
                    ComponentName cn = ComponentName.unflattenFromString(value);
                    if (cn == null) throw new IllegalArgumentException("Bad component name: " + value);
                    intent.putExtra(key, cn);
                    break;
                }
                case "--eia": {
                    String key = cmd.getNextArgRequired();
                    String[] strings = cmd.getNextArgRequired().split(",");
                    int[] list = new int[strings.length];
                    for (int i = 0; i < strings.length; i++) list[i] = Integer.decode(strings[i]);
                    intent.putExtra(key, list);
                    break;
                }
                case "--eial": {
                    String key = cmd.getNextArgRequired();
                    String[] strings = cmd.getNextArgRequired().split(",");
                    ArrayList<Integer> list = new ArrayList<>(strings.length);
                    for (String s : strings) list.add(Integer.decode(s));
                    intent.putExtra(key, list);
                    break;
                }
                case "--el": {
                    String key = cmd.getNextArgRequired();
                    intent.putExtra(key, Long.valueOf(cmd.getNextArgRequired()));
                    break;
                }
                case "--ela": {
                    String key = cmd.getNextArgRequired();
                    String[] strings = cmd.getNextArgRequired().split(",");
                    long[] list = new long[strings.length];
                    for (int i = 0; i < strings.length; i++) list[i] = Long.valueOf(strings[i]);
                    intent.putExtra(key, list);
                    hasIntentInfo = true;
                    break;
                }
                case "--elal": {
                    String key = cmd.getNextArgRequired();
                    String[] strings = cmd.getNextArgRequired().split(",");
                    ArrayList<Long> list = new ArrayList<>(strings.length);
                    for (String s : strings) list.add(Long.valueOf(s));
                    intent.putExtra(key, list);
                    hasIntentInfo = true;
                    break;
                }
                case "--ef": {
                    String key = cmd.getNextArgRequired();
                    intent.putExtra(key, Float.valueOf(cmd.getNextArgRequired()));
                    hasIntentInfo = true;
                    break;
                }
                case "--efa": {
                    String key = cmd.getNextArgRequired();
                    String[] strings = cmd.getNextArgRequired().split(",");
                    float[] list = new float[strings.length];
                    for (int i = 0; i < strings.length; i++) list[i] = Float.valueOf(strings[i]);
                    intent.putExtra(key, list);
                    hasIntentInfo = true;
                    break;
                }
                case "--efal": {
                    String key = cmd.getNextArgRequired();
                    String[] strings = cmd.getNextArgRequired().split(",");
                    ArrayList<Float> list = new ArrayList<>(strings.length);
                    for (String s : strings) list.add(Float.valueOf(s));
                    intent.putExtra(key, list);
                    hasIntentInfo = true;
                    break;
                }
                case "--esa": {
                    String key = cmd.getNextArgRequired();
                    String[] strings = cmd.getNextArgRequired().split("(?<!\\\\),");
                    for (int i = 0; i < strings.length; i++) strings[i] = strings[i].replaceAll("\\\\,", ",");
                    intent.putExtra(key, strings);
                    hasIntentInfo = true;
                    break;
                }
                case "--esal": {
                    String key = cmd.getNextArgRequired();
                    String[] strings = cmd.getNextArgRequired().split("(?<!\\\\),");
                    ArrayList<String> list = new ArrayList<>(strings.length);
                    for (String s : strings) list.add(s.replaceAll("\\\\,", ","));
                    intent.putExtra(key, list);
                    hasIntentInfo = true;
                    break;
                }
                case "--ez": {
                    String key = cmd.getNextArgRequired();
                    String value = cmd.getNextArgRequired().toLowerCase();
                    boolean arg;
                    if ("true".equals(value) || "t".equals(value)) {
                        arg = true;
                    } else if ("false".equals(value) || "f".equals(value)) {
                        arg = false;
                    } else {
                        try {
                            arg = Integer.decode(value) != 0;
                        } catch (NumberFormatException ex) {
                            throw new IllegalArgumentException("Invalid boolean value: " + value);
                        }
                    }
                    intent.putExtra(key, arg);
                    break;
                }
                case "-n": {
                    String str = cmd.getNextArgRequired();
                    ComponentName cn = ComponentName.unflattenFromString(str);
                    if (cn == null) throw new IllegalArgumentException("Bad component name: " + str);
                    intent.setComponent(cn);
                    if (intent == baseIntent) hasIntentInfo = true;
                    break;
                }
                case "-p": {
                    intent.setPackage(cmd.getNextArgRequired());
                    if (intent == baseIntent) hasIntentInfo = true;
                    break;
                }
                case "-f":
                    intent.setFlags(Integer.decode(cmd.getNextArgRequired()));
                    break;
                case "--grant-read-uri-permission":
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    break;
                case "--grant-write-uri-permission":
                    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    break;
                case "--grant-persistable-uri-permission":
                    intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                    break;
                case "--grant-prefix-uri-permission":
                    intent.addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
                    break;
                case "--exclude-stopped-packages":
                    intent.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
                    break;
                case "--include-stopped-packages":
                    intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                    break;
                case "--debug-log-resolution":
                    intent.addFlags(Intent.FLAG_DEBUG_LOG_RESOLUTION);
                    break;
                case "--activity-brought-to-front":
                    intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
                    break;
                case "--activity-clear-top":
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    break;
                case "--activity-clear-when-task-reset":
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                    break;
                case "--activity-exclude-from-recents":
                    intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    break;
                case "--activity-launched-from-history":
                    intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
                    break;
                case "--activity-multiple-task":
                    intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                    break;
                case "--activity-no-animation":
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    break;
                case "--activity-no-history":
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    break;
                case "--activity-no-user-action":
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
                    break;
                case "--activity-previous-is-top":
                    intent.addFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
                    break;
                case "--activity-reorder-to-front":
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    break;
                case "--activity-reset-task-if-needed":
                    intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    break;
                case "--activity-single-top":
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    break;
                case "--activity-clear-task":
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    break;
                case "--activity-task-on-home":
                    intent.addFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME);
                    break;
                case "--receiver-registered-only":
                    intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
                    break;
                case "--receiver-replace-pending":
                    intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
                    break;
                case "--receiver-foreground":
                    intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                    break;
                case "--receiver-no-abort":
                    intent.addFlags(Intent.FLAG_RECEIVER_NO_ABORT);
                    break;
                case "--selector":
                    intent.setDataAndType(data, type);
                    intent = new Intent();
                    break;
                default:
                    if (optionHandler != null && optionHandler.handleOption(opt, cmd)) {
                        // caller handled
                    } else {
                        throw new IllegalArgumentException("Unknown option: " + opt);
                    }
                    break;
            }
        }
        intent.setDataAndType(data, type);

        final boolean hasSelector = intent != baseIntent;
        if (hasSelector) {
            baseIntent.setSelector(intent);
            intent = baseIntent;
        }

        String arg = cmd.getNextArg();
        baseIntent = null;
        if (arg == null) {
            if (hasSelector) {
                baseIntent = new Intent(Intent.ACTION_MAIN);
                baseIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            }
        } else if (arg.indexOf(':') >= 0) {
            baseIntent = Intent.parseUri(arg, Intent.URI_INTENT_SCHEME
                    | Intent.URI_ANDROID_APP_SCHEME | Intent.URI_ALLOW_UNSAFE);
        } else if (arg.indexOf('/') >= 0) {
            baseIntent = new Intent(Intent.ACTION_MAIN);
            baseIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            baseIntent.setComponent(ComponentName.unflattenFromString(arg));
        } else {
            baseIntent = new Intent(Intent.ACTION_MAIN);
            baseIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            baseIntent.setPackage(arg);
        }
        if (baseIntent != null) {
            Bundle extras = intent.getExtras();
            intent.replaceExtras((Bundle) null);
            Bundle uriExtras = baseIntent.getExtras();
            baseIntent.replaceExtras((Bundle) null);
            if (intent.getAction() != null && baseIntent.getCategories() != null) {
                HashSet<String> cats = new HashSet<>(baseIntent.getCategories());
                for (String c : cats) baseIntent.removeCategory(c);
            }
            intent.fillIn(baseIntent, Intent.FILL_IN_COMPONENT | Intent.FILL_IN_SELECTOR);
            if (extras == null) {
                extras = uriExtras;
            } else if (uriExtras != null) {
                uriExtras.putAll(extras);
                extras = uriExtras;
            }
            intent.replaceExtras(extras);
            hasIntentInfo = true;
        }

        if (!hasIntentInfo) throw new IllegalArgumentException("No intent supplied");
        return intent;
    }

    public static void printIntentArgsHelp(PrintWriter pw, String prefix) {
        final String[] lines = new String[]{
                "<INTENT> specifications include these flags and arguments:",
                "    [-a <ACTION>] [-d <DATA_URI>] [-t <MIME_TYPE>]",
                "    [-c <CATEGORY> [-c <CATEGORY>] ...]",
                "    [-e|--es <EXTRA_KEY> <EXTRA_STRING_VALUE> ...]",
                "    [--esn <EXTRA_KEY> ...]",
                "    [--ez <EXTRA_KEY> <EXTRA_BOOLEAN_VALUE> ...]",
                "    [--ei <EXTRA_KEY> <EXTRA_INT_VALUE> ...]",
                "    [--el <EXTRA_KEY> <EXTRA_LONG_VALUE> ...]",
                "    [--ef <EXTRA_KEY> <EXTRA_FLOAT_VALUE> ...]",
                "    [--eu <EXTRA_KEY> <EXTRA_URI_VALUE> ...]",
                "    [--ecn <EXTRA_KEY> <EXTRA_COMPONENT_NAME_VALUE>]",
                "    [--eia <EXTRA_KEY> <EXTRA_INT_VALUE>[,<EXTRA_INT_VALUE...]]",
                "    [--eial <EXTRA_KEY> <EXTRA_INT_VALUE>[,<EXTRA_INT_VALUE...]]",
                "    [--ela <EXTRA_KEY> <EXTRA_LONG_VALUE>[,<EXTRA_LONG_VALUE...]]",
                "    [--elal <EXTRA_KEY> <EXTRA_LONG_VALUE>[,<EXTRA_LONG_VALUE...]]",
                "    [--efa <EXTRA_KEY> <EXTRA_FLOAT_VALUE>[,<EXTRA_FLOAT_VALUE...]]",
                "    [--efal <EXTRA_KEY> <EXTRA_FLOAT_VALUE>[,<EXTRA_FLOAT_VALUE...]]",
                "    [--esa <EXTRA_KEY> <EXTRA_STRING_VALUE>[,<EXTRA_STRING_VALUE...]]",
                "    [--esal <EXTRA_KEY> <EXTRA_STRING_VALUE>[,<EXTRA_STRING_VALUE...]]",
                "    [-f <FLAG>]",
                "    [--grant-read-uri-permission] [--grant-write-uri-permission]",
                "    [--activity-clear-top] [--activity-single-top]",
                "    [--receiver-registered-only] [--receiver-replace-pending]",
                "    [--selector]",
                "    [<URI> | <PACKAGE> | <COMPONENT>]"
        };
        for (String line : lines) {
            pw.print(prefix);
            pw.println(line);
        }
    }
}
