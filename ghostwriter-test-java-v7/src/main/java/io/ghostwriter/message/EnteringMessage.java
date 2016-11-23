package io.ghostwriter.message;

public class EnteringMessage extends Message<EnteringMessage.BasePayload> {

    public EnteringMessage(Object source, String method) {
        this(source, method, NoParameters.INSTANCE);
    }

    public EnteringMessage(Object source,  String method, BasePayload payload) {
        super(source, method, payload);
    }

    public boolean hasParameters() {
        BasePayload payload = getPayload();
        return !NoParameters.INSTANCE.equals(payload);
    }

    @Override
    public String toString() {
        return super.toString() + getPayload().toString();
    }

    public static abstract class BasePayload {

        protected BasePayload() {
        }

        public abstract Object[] getParameters();

    }

    // cannot use enum for the singleton pattern since that doesn't support 'extend'
    protected static class NoParameters extends BasePayload {
        public static NoParameters INSTANCE = new NoParameters();

        private NoParameters() {
        }

        @Override
        public Object[] getParameters() {
            return null;
        }

        @Override
        public String toString() {
            return "()";
        }
    }

    public static class Payload extends BasePayload {

        final private Object[] parameters;

        public Payload(Object[] parameters) {
            this.parameters = parameters;
        }

        public Object[] getParameters() {
            return parameters;
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(super.toString()).append("(");
            final int UPPER_BOUND = parameters.length - 1;
            final int SIZE_OF_ENTRY_PAIRS = 2;
            for (int i = 0; i < UPPER_BOUND; i += SIZE_OF_ENTRY_PAIRS) {
                Object name = parameters[i];
                Object value = parameters[i + 1];
                sb.append(name).append(" = ").append(value);
                if (i != UPPER_BOUND - 1) {
                    sb.append(", ");
                }
            }
            sb.append(")");

            return sb.toString();
        }
    }
}
