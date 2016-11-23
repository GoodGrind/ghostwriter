package io.ghostwriter.message;

import java.util.Objects;

public class TimeoutMessage extends Message<Object[]> {

    public TimeoutMessage(Object source, String method, Object[] payload) {
        super(source, method, payload);
        final int EXPECTED_PAYLOAD_SIZE = 2; // timeoutThreshold, timeout
        if (Objects.requireNonNull(payload).length != EXPECTED_PAYLOAD_SIZE) {
            throw new IllegalStateException("Expected payload size of '" + EXPECTED_PAYLOAD_SIZE + "', got: " + payload.length);
        }
    }

    public long getThreshold() {
        final Object[] payload = this.getPayload();
        final int INDEX_OF_THRESHOLD_VALUE = 0;
        return (long) payload[INDEX_OF_THRESHOLD_VALUE];
    }

    public long getTimeout() {
        final Object[] payload = this.getPayload();
        final int INDEX_OF_TIMEOUT_VALUE = 1;
        return (long) payload[INDEX_OF_TIMEOUT_VALUE];
    }

}
