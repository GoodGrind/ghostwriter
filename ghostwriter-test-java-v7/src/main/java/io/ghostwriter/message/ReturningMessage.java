package io.ghostwriter.message;

public class ReturningMessage extends Message<Object> {

    public ReturningMessage(Object source, String method, Object result) {
        super(source, method, result);
    }

    @Override
    public String toString() {
        String msg = super.toString();
        StringBuffer sb = new StringBuffer();
        sb.append(msg).append(" = ").append(String.valueOf(getPayload()));

        return sb.toString();
    }

}
