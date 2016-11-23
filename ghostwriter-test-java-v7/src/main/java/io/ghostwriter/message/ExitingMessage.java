package io.ghostwriter.message;

public class ExitingMessage extends Message<ExitingMessage.NoPayload> {

    public ExitingMessage(Object source, String method) {
        super(source, method, NoPayload.INSTANCE);
    }

    @Override
    public String toString() {
        String msg = super.toString();
        return msg + "}";
    }

    enum  NoPayload {
        INSTANCE
    }
}
