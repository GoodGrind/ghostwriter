package io.ghostwriter.message;

public class OnErrorMessage extends Message<Throwable> {

    public OnErrorMessage(Object source, String method, Throwable payload) {
        super(source, method, payload);
    }

    @Override
    public String toString() {
        return super.toString() + ": threw " + getPayload().toString();
    }
}
