package io.ghostwriter;

import io.ghostwriter.message.EnteringMessage;
import io.ghostwriter.message.ExitingMessage;
import io.ghostwriter.message.Message;
import io.ghostwriter.message.OnErrorMessage;
import io.ghostwriter.test.MessageSequenceAsserter;
import io.ghostwriter.test.Parameter;
import io.ghostwriter.test.TestBase;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class SourceLineMappingTest extends TestBase {

    private void methodWithAnException() {
        throw new IllegalStateException("Some exception");
    }

    @Test
    public void testSourceLineMapping() {
        InMemoryTracer inMemoryTracer = fetchedPreparedInMemoryTracer();
        IllegalStateException caughtException = null;
        try {
            methodWithAnException();
        }
        catch (IllegalStateException ex) {
            caughtException = ex;
        }

        assertTrue("Expected exception not caught!", caughtException != null);
        final StackTraceElement[] stackTrace = caughtException.getStackTrace();
        final StackTraceElement exceptionElementInCalledMethod = stackTrace[0];
        final StackTraceElement exceptionElementInCallerMethod = stackTrace[1];
        // line number where we originally trigger an exception: 'throw new IllegalStateException("Some exception");'
        final int exceptionSourceLineNumberInCalledMethod = 17;
        // line number where we indirectly trigger an exception by calling a "faulty" method: 'methodWithAnException();'
        final int exceptionSourceLineNumberInCallerMethod = 25;

        int lineNumber = exceptionElementInCalledMethod.getLineNumber();
        assertTrue("Exception doesn't refer to trigger line in called method! Expected " + exceptionSourceLineNumberInCalledMethod + ", got: " + lineNumber,
                lineNumber == exceptionSourceLineNumberInCalledMethod);

        lineNumber = exceptionElementInCallerMethod.getLineNumber();
        assertTrue("Exception doesn't refer to trigger line in caller method! Expected " + exceptionSourceLineNumberInCallerMethod + ", got: " + lineNumber,
                lineNumber == exceptionSourceLineNumberInCallerMethod);

        final Message<?> exitingMessage = inMemoryTracer.popMessage();
        // verify exitingMessage type
        boolean isExitingMessage = exitingMessage instanceof ExitingMessage;
        assertTrue("Invalid exitingMessage type: " + exitingMessage.getClass(), isExitingMessage);

        final ExitingMessage exitingMsg = (ExitingMessage) exitingMessage;
        final String methodName = exitingMsg.getMethod();
        assertTrue("Exiting message came from method: " + methodName + ", expected 'methodWithAnException'",
                "methodWithAnException".equals(methodName));

        // tracing should catch the "unexpected" error and generate the appropriate message
        final Message<?> onErrorMessage = inMemoryTracer.popMessage();
        boolean isOnErrorMessage = onErrorMessage instanceof OnErrorMessage;
        assertTrue("Invalid on error message type: " + onErrorMessage.getClass(), isOnErrorMessage);

        // check that the caught exception of GW is the same as the "expected one"
        OnErrorMessage onErrorMsg = (OnErrorMessage) onErrorMessage;
        final Throwable gwCaughtException = onErrorMsg.getPayload();
        assertTrue("Caught exception of GW does not match the expected one",
                gwCaughtException.equals(caughtException));

        final Message<?> enteringMessage = inMemoryTracer.popMessage();
        // verify enteringMessage type
        boolean isEnteringMessage = enteringMessage instanceof EnteringMessage;
        assertTrue("Invalid entering message type: " + enteringMessage.getClass(), isEnteringMessage);
    }

    @Test
    public void testReturnExpressionWithException() {
        final InMemoryTracer inMemoryTracer = fetchedPreparedInMemoryTracer();
        inMemoryTracer.clearMessages();
        NullPointerException expectedException = null;
        try {
            returnExpressionWithException(23);
        }
        catch (NullPointerException npe) {
            expectedException = npe;
        }

        assertTrue("Expected exception not caught!", expectedException != null);
        MessageSequenceAsserter.messageSequence()
                .entering("returnExpressionWithException", new Parameter<>("someInt", 23))
                .onError("returnExpressionWithException", NullPointerException.class)
                .exiting("returnExpressionWithException")
                .empty();

        final StackTraceElement[] stackTrace = expectedException.getStackTrace();
        final StackTraceElement exceptionElementInCalledMethod = stackTrace[0];
        final StackTraceElement exceptionElementInCallerMethod = stackTrace[1];

        // line number where we originally trigger an exception: 'null.toString();'
        final int exceptionSourceLineNumberInCalledMethod = 114;
        // line number where we indirectly trigger an exception by calling a "faulty" method: 'returnExpressionWithException(23);'
        final int exceptionSourceLineNumberInCallerMethod = 81;

        int lineNumber = exceptionElementInCalledMethod.getLineNumber();
        assertTrue("Exception doesn't refer to trigger line in called method! Expected " + exceptionSourceLineNumberInCalledMethod + ", got: " + lineNumber,
                lineNumber == exceptionSourceLineNumberInCalledMethod);

        lineNumber = exceptionElementInCallerMethod.getLineNumber();
        assertTrue("Exception doesn't refer to trigger line in caller method! Expected " + exceptionSourceLineNumberInCallerMethod + ", got: " + lineNumber,
                lineNumber == exceptionSourceLineNumberInCallerMethod);
    }

    public String returnExpressionWithException(Integer someInt) {
        String a = null;
        return a.toString();
    }

}
