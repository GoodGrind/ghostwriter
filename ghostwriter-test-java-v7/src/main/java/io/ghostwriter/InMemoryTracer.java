package io.ghostwriter;

import io.ghostwriter.message.*;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Queue;

public class InMemoryTracer implements Tracer {

    private final Queue<Message<?>> messageStack;
    // by default we don't want to generate more messages then necessary for testing
    private boolean doTrackValueChanges = false;

    private boolean doTrackEnteringExiting = true;

    public InMemoryTracer() {
        messageStack = Collections.asLifoQueue(new ArrayDeque<Message<?>>());
    }

    public Message<?> popMessage() {
        return messageStack.remove();
    }

    public void pushMessage(Message<?> msg) {
        messageStack.add(msg);
    }

    public void clearMessages() {
        messageStack.clear();
    }

    public void enableValueChangeTracking() {
        doTrackValueChanges = true;
    }

    public void disableValueChangeTracking() {
        doTrackValueChanges = false;
    }

    public void enableEnteringExitingTracking() {
        doTrackEnteringExiting = true;
    }

    public void disableEnteringExitingTracking() {
        doTrackEnteringExiting = false;
    }

    public int numberOfMessages() {
        return messageStack.size();
    }

    @Override
    public void entering(Object source, String method, Object... params) {
        if (!doTrackEnteringExiting) {
            return;
        }

        EnteringMessage enteringMessage;
        if (params.length == 0) {
            enteringMessage = new EnteringMessage(source, method);
        }
        else {
            EnteringMessage.Payload payload = new EnteringMessage.Payload(params);
            enteringMessage = new EnteringMessage(source, method, payload);
        }

        pushMessage(enteringMessage);
    }

    @Override
    public void exiting(Object source, String method) {
        if (!doTrackEnteringExiting) {
            return;
        }

        ExitingMessage exitingMessage = new ExitingMessage(source, method);
        pushMessage(exitingMessage);
    }

    @Override
    public void valueChange(Object source, String method, String variable, Object newValue) {
        if (!doTrackValueChanges) {
            return;
        }

        ValueChangeMessage valueChangeMessage = new ValueChangeMessage(source, method, new ValueChangeMessage.Payload(variable, newValue));
        pushMessage(valueChangeMessage);
    }

    @Override
    public <T> void returning(Object source, String method, T returnValue) {
        ReturningMessage returningMessage = new ReturningMessage(source, method, returnValue);
        pushMessage(returningMessage);
    }

    @Override
    public void onError(Object source, String method, Throwable error) {
        OnErrorMessage onErrorMessage = new OnErrorMessage(source, method, error);
        pushMessage(onErrorMessage);
    }

    @Override
    public void timeout(Object source, String method, long timeoutThreshold, long timeout) {
        TimeoutMessage timeoutMessage = new TimeoutMessage(source, method, new Object[]{timeoutThreshold, timeout});
        pushMessage(timeoutMessage);
    }
}
