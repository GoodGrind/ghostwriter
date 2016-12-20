package io.ghostwriter.openjdk.v7.common;

import io.ghostwriter.GhostWriter;

public enum RuntimeHandler {
    ENTERING(GhostWriter.class.getName() + ".entering"),
    RETURNING(GhostWriter.class.getName() + ".returning"),
    EXITING(GhostWriter.class.getName() + ".exiting"),
    VALUE_CHANGE(GhostWriter.class.getName() + ".valueChange"),
    ON_ERROR(GhostWriter.class.getName() + ".onError"),
    TIMEOUT(GhostWriter.class.getName() + ".timeout");

    private final String fullyQualifiedName;

    RuntimeHandler(String fqn) {
        fullyQualifiedName = fqn;
    }

    // NOTE (snorbi07): using toString instead of getFullyQualifiedName might not be the best idea.
    @Override
    public String toString() {
        return fullyQualifiedName;
    }

}
