package io.ghostwriter.test;

import io.ghostwriter.InMemoryTracer;
import io.ghostwriter.InMemoryTracerProvider;
import io.ghostwriter.message.*;

import java.util.*;

import static org.junit.Assert.assertTrue;


public class MessageSequenceAsserter {

    public static final int NUMBER_OF_ENTRIES_PER_TRACED_PARAMETER = 2; // first value should be the name, the second one is the value of the parameter

    private final Iterator<Message<?>> messages;

    static private InMemoryTracer getTracer() {
        return InMemoryTracerProvider.INSTANCE.getTracer();
    }

    private MessageSequenceAsserter(List<Message<?>> listOfMessages) {
        messages = listOfMessages.iterator();
    }

    private Message<?> nextMessage() {
        assertTrue("Message sequence is empty!", messages.hasNext());
        return messages.next();
    }

    public MessageSequenceAsserter empty() {
        assertTrue("Message sequence is not empty!", !messages.hasNext());
        return this;
    }

    public MessageSequenceAsserter entering(String method) {
        Message<?> message = nextMessage();
        assertEnteringMessage(message, method);
        return this;
    }

    public <T> MessageSequenceAsserter entering(String method, Parameter<T> param) {
        Message<?> message = nextMessage();
        assertEnteringMessage(message, method, param);
        return this;
    }

    public MessageSequenceAsserter entering(String method, List<Parameter<?>> params) {
        Message<?> message = nextMessage();
        assertEnteringMessage(message, method, params);
        return this;
    }

    public MessageSequenceAsserter entering(String method, Parameter<?>... params) {
        return entering(method, Arrays.asList(params));
    }

    public <T> MessageSequenceAsserter returning(String method, T result) {
        Message<?> message = nextMessage();
        assertReturningMessage(message, method, result);
        return this;
    }

    public MessageSequenceAsserter exiting(String method) {
        Message<?> message = nextMessage();
        assertExitingMessage(message, method);
        return this;
    }

    public <T> MessageSequenceAsserter valueChange(String method, String valueName, T newValue) {
        Message<?> message = nextMessage();
        assertValueChangeMessage(message, method, valueName, newValue);
        return this;
    }

    public MessageSequenceAsserter onError(String method, Class<? extends Throwable> errorType) {
        Message<?> message = nextMessage();
        assertOnErrorMessage(message, method, errorType);
        return this;
    }

    public MessageSequenceAsserter timeout(String method, long threshold) {
        Message<?> message = nextMessage();
        assertTimeoutMessage(message, method, threshold);
        return this;
    }

    public MessageSequenceAsserter any() {
        nextMessage();
        return this;
    }

    public static MessageSequenceAsserter messageSequence() {
        return messageSequence(getTracer());
    }

    private static MessageSequenceAsserter messageSequence(InMemoryTracer tracer) {
        // Sine the InMemoryTracer contains the messages in a LIFO queue, we need to reverse
        // those to have nice, event triggering order based assertations.
        // This can be refactored once all tests have been moved to the new test API.

        Stack<Message<?>> s = new Stack<>();
        while(tracer.numberOfMessages() > 0) {
            s.push(tracer.popMessage());
        }

        List<Message<?>> messages = new ArrayList<>();
        while(!s.isEmpty()) {
            messages.add(s.pop());
        }

        return new MessageSequenceAsserter(messages);
    }

    private void assertEnteringMessage(Message<?> msg, String method, Parameter<?> param) {
        final EnteringMessage enteringMessage = assertEnteringMessageType(msg, method);

        // verify that method has parameters
        assertTrue("Method has no traced parameters!", enteringMessage.hasParameters());

        Object[] parameters = enteringMessage.getPayload().getParameters();

        // verify that we have the correct number of traced parameters
        int numberOfParameters = parameters.length / NUMBER_OF_ENTRIES_PER_TRACED_PARAMETER;
        assertTrue("Expected number of parameters: 1, got: " + numberOfParameters, numberOfParameters == 1);

        // verify that we have the expected name and value
        String parameterName = (String) parameters[0];
        Object parameterValue = parameters[1];
        assertEnteringMessageParameter(param, parameterName, parameterValue);
    }

    private void assertEnteringMessage(Message<?> msg, String method, List<Parameter<?>> parameters) {
        final EnteringMessage enteringMessage = assertEnteringMessageType(msg, method);

        // verify that method has parameters
        assertTrue("Method has no traced parameters!", enteringMessage.hasParameters());

        Object[] tracedParameters = enteringMessage.getPayload().getParameters();

        // verify that we have the correct number of traced parameters
        int numberOfParameters = tracedParameters.length / NUMBER_OF_ENTRIES_PER_TRACED_PARAMETER;
        assertTrue("Expected number of parameters: " + parameters.size() + ", got: " + numberOfParameters, numberOfParameters == parameters.size());

        for (int i = 0; i < parameters.size() - 1; i++) {
            Parameter<?> expectedParameter = parameters.get(i);

            int offset = i * NUMBER_OF_ENTRIES_PER_TRACED_PARAMETER;
            String tracedParameterName = (String) tracedParameters[offset];
            Object tracedParameterValue = tracedParameters[offset + 1];

            assertEnteringMessageParameter(expectedParameter, tracedParameterName, tracedParameterValue);
        }
    }

    private void assertEnteringMessageParameter(Parameter<?> param, String parameterName, Object parameterValue) {
        // verify parameter name is correct
        boolean parameterHasCorrectName = parameterName.equals(param.getName());
        assertTrue("Expected parameter name: " + param.getName() + ", got: " + parameterName, parameterHasCorrectName);

        // verify parameter type is correct
        Class<?> parameterType = parameterValue.getClass();
        Object expectedParameterValue = param.getValue();
        Class<?> expectedParameterType = expectedParameterValue.getClass();
        boolean parameterHasCorrectType = parameterType.equals(expectedParameterType);
        assertTrue("Expected parameter type: " + expectedParameterType.getName() + ", got: " + parameterType.getName(), parameterHasCorrectType);

        // verify that parameter value is correct
        boolean parameterHasCorrectValue = expectedParameterValue.equals(parameterValue);
        assertTrue("Expected parameter value: " + expectedParameterValue + ", got: " + parameterValue, parameterHasCorrectValue);
    }

    private void assertEnteringMessage(Message<?> msg, String method) {
        final EnteringMessage enteringMessage = assertEnteringMessageType(msg, method);

        // verify that method has no parameters
        assertTrue("Method has traced parameters!", !enteringMessage.hasParameters());
    }

    private EnteringMessage assertEnteringMessageType(Message<?> msg, String method) {
        // verify enterinMessage type
        boolean isEnteringMessage = msg instanceof EnteringMessage;
        assertTrue("Expected message type: " + EnteringMessage.class.getName() + ", got: " + msg.toString(), isEnteringMessage);

        final EnteringMessage enteringMessage = (EnteringMessage) msg;

        // verify method name
        final String enteredMethodName = enteringMessage.getMethod();
        boolean isCorrectMethodName = method.equals(enteredMethodName);
        assertTrue("Expected method name: " + method + ", got: " + enteredMethodName, isCorrectMethodName);
        return enteringMessage;
    }

    private <T> void assertReturningMessage(Message<?> msg, String method, T result) {
        boolean isReturningMessage = msg instanceof ReturningMessage;
        assertTrue("Expected message type: " + ReturningMessage.class.getName() + ", got: " + msg.toString(), isReturningMessage);

        final ReturningMessage returningMessage = (ReturningMessage) msg;

        // verify method name
        final String returningMethodName = returningMessage.getMethod();
        boolean isCorrectMethodName = method.equals(returningMethodName);
        assertTrue("Expected method name: " + method + ", got: " + returningMethodName, isCorrectMethodName);


        // verify that result value has the correct type
        Object capturedResult = returningMessage.getPayload();
        if (result != null && capturedResult != null) {
            Class<?> capturedResultType = capturedResult.getClass();
            Class<?> expectedResultType = result.getClass();
            boolean hasCorrectType = capturedResultType.equals(expectedResultType);
            assertTrue("Expected result type of: " + expectedResultType + ", got: " + capturedResultType, hasCorrectType);
        }

        // verify the result value has the correct value
        boolean hasCorrectValue = result == capturedResult;
        if (result != null) {
            hasCorrectValue = result.equals(capturedResult);
        }
        assertTrue("Expected result value: " + result + ", got: " + capturedResult, hasCorrectValue);
    }

    private void assertExitingMessage(Message<?> msg, String method) {
        // verify exitingMessage type
        boolean isExitingMessage = msg instanceof ExitingMessage;
        assertTrue("Expected message type: " + ExitingMessage.class.getName() + ", got: " + msg.toString(), isExitingMessage);

        final ExitingMessage exitingMessage = (ExitingMessage) msg;

        // verify method name
        final String exitedMethodName = exitingMessage.getMethod();
        boolean isCorrectMethodName = method.equals(exitedMethodName);
        assertTrue("Expected method name: " + method + ", got: " + exitedMethodName, isCorrectMethodName);
    }

    private <T> void assertValueChangeMessage(Message<?> msg, String method, String value, T expectedValue) {
        // verify value change message type
        boolean isValueChangeMessage = msg instanceof ValueChangeMessage;
        assertTrue("Expected message type: " + ValueChangeMessage.class.getName() + ", got: " + msg.toString(), isValueChangeMessage);

        final ValueChangeMessage valueChangeMessage = (ValueChangeMessage) msg;

        // verify method name
        final String valueChangeMessageMethod = valueChangeMessage.getMethod();
        boolean isCorrectMethodName = method.equals(valueChangeMessageMethod);
        assertTrue("Expected method name: " + method + ", got: " + valueChangeMessageMethod, isCorrectMethodName);


        // verify that the traced value has the correct name
        String tracedValueName = valueChangeMessage.getPayload().getName();
        boolean isExpectedValueName = value.equals(tracedValueName);
        assertTrue("Incorrect value name, expected: " + value + ", got: " + tracedValueName, isExpectedValueName);

        // verify that traced value has the correct type
        Object tracedValue = valueChangeMessage.getPayload().getNewValue();
        Class<?> expectedValueType = expectedValue == null ? null : expectedValue.getClass();
        Class<?> tracedValueType = tracedValue == null ? null : tracedValue.getClass();
        boolean hasCorrectValueType = (tracedValueType == null || expectedValueType == null) || expectedValueType.equals(tracedValueType);
        assertTrue("Expected traced value type of: " + expectedValueType + ", got: " + tracedValueType, hasCorrectValueType);

        // verify the tracedValue value has the correct value
        boolean valuesAreEqual = expectedValue != null ? expectedValue.equals(tracedValue) : (tracedValue == null || tracedValue.equals(expectedValue));
        assertTrue("Expected traced value value: " + expectedValue + ", got: " + tracedValue, valuesAreEqual);
    }

    private void assertOnErrorMessage(Message<?> message, String method, Class<? extends Throwable> errorType) {
        // verify value change message type
        boolean isErrorMessage = message instanceof OnErrorMessage;
        assertTrue("Expected message type: " + OnErrorMessage.class.getName() + ", got: " + message.toString(), isErrorMessage);

        final OnErrorMessage onErrorMessage = (OnErrorMessage) message;

        // verify method name
        final String methodName = onErrorMessage.getMethod();
        boolean isCorrectMethodName = method.equals(methodName);
        assertTrue("Expected method name: " + method + ", got: " + methodName, isCorrectMethodName);

        // verify that error the correct type
        final Throwable capturedError = onErrorMessage.getPayload();
        Class<?> causeType = capturedError.getClass();
        boolean hasCorrectValueType = errorType.equals(causeType);
        assertTrue("Expected error type of: " + errorType.toString() + ", got: " + capturedError.getClass().toString(), hasCorrectValueType);
    }


    private void assertTimeoutMessage(Message<?> msg, String method, long expectedThreshold) {
        // verify value change message type
        boolean isTimeoutMessage = msg instanceof TimeoutMessage;
        assertTrue("Expected message type: " + TimeoutMessage.class.getName() + ", got: " + msg.getClass().getName(), isTimeoutMessage);

        final TimeoutMessage timeoutMessage = (TimeoutMessage) msg;

        // verify method name
        final String timeoutMessageMethod = timeoutMessage.getMethod();
        boolean isCorrectMethodName = method.equals(timeoutMessageMethod);
        assertTrue("Expected method name: " + method + ", got: " + timeoutMessageMethod, isCorrectMethodName);

        final long threshold = timeoutMessage.getThreshold();
        assertTrue("Expected threshold value: " + expectedThreshold + ", got: " + threshold, expectedThreshold == threshold);

        final long timeout = timeoutMessage.getTimeout();
        assertTrue("Expected timeout value: " + timeout + ", to be smaller than: " + expectedThreshold, expectedThreshold < timeout);
    }

}


