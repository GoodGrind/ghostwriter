package io.ghostwriter.openjdk.v7.common;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;

public enum Logger {
    ;

    private static Messager messager;

    private static boolean doVerboseLogging = false;

    private static String format(Class<?> klass, String method, String message) {
        final int INITIAL_CAPACITY = 32;
        StringBuilder sb = new StringBuilder(INITIAL_CAPACITY);
        if (klass != null) {
            sb.append(klass.getName());
            sb.append(".");
        }
        sb.append(method);
        sb.append(": ");
        sb.append(message);

        return sb.toString();
    }

    public static void note(Class<?> type, String method, String message) {
        if (!doVerboseLogging) {
            return;
        }

        validateState();
        String output = format(type, method, message);
        messager.printMessage(Diagnostic.Kind.NOTE, output);
    }

    public static void warning(Class<?> type, String method, String message) {
        validateState();
        String output = format(type, method, message);
        messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING, output);
    }

    /**
     * Display an error message.
     *
     * @param type    - class that produced the error
     * @param method  - method that produced the error
     * @param message - error description
     */
    public static void error(Class<?> type, String method, String message) {
        validateState();
        String output = format(type, method, message);
        messager.printMessage(Diagnostic.Kind.ERROR, output);
    }

    public static void initialize(Messager msg, boolean verbose) {
        if (msg == null) {
            throw new IllegalArgumentException("Cannot initialize with null!");
        }
        messager = msg;
        doVerboseLogging = verbose;
    }

    private static void validateState() {
        if (messager == null) {
            throw new IllegalStateException("Logger has not been initialized!");
        }
    }

}
