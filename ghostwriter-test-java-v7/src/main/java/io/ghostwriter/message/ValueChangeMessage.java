package io.ghostwriter.message;

public class ValueChangeMessage extends Message<ValueChangeMessage.Payload> {

    public ValueChangeMessage(Object source, String method, Payload payload) {
        super(source, method, payload);
    }

    @Override
    public String toString() {
        return super.toString() + ": " + getPayload().toString();
    }

    public static class Payload {

        final private String name;

        final private Object newValue;

        public Payload(String name, Object newValue) {
            this.name = name;
            this.newValue = newValue;
        }

        public String getName() {
            return name;
        }

        public Object getNewValue() {
            return newValue;
        }

        @Override
        public String toString() {
            return super.toString() + name + " = " + String.valueOf(newValue);
        }
    }
}
