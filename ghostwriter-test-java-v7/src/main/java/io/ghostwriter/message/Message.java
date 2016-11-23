package io.ghostwriter.message;

public class Message<T> {

    final private Object source;

    final private String method;

    final private T payload;

    public Message(Object source, String method, T payload) {
        this.source = source;
        this.method = method;
        this.payload = payload;
    }

    public Object getSource() {
        return source;
    }

    public String getMethod() {
        return method;
    }

    public T getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.getClass().getSimpleName().toString())
//                .append(String.valueOf(source))
                .append(".")
                .append(method);

        return sb.toString();
    }
}
