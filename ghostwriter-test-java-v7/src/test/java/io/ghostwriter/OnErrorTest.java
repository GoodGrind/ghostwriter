package io.ghostwriter;

import io.ghostwriter.message.Message;
import io.ghostwriter.message.OnErrorMessage;
import io.ghostwriter.test.TestBase;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class OnErrorTest extends TestBase {

    @Test
    public void testDereferenceNull() {
        InMemoryTracer inMemoryTracer = fetchedPreparedInMemoryTracer();
        boolean expectedExceptionTypeCaught = false;
        try {
            dereferenceNull();
        }
        catch (NullPointerException e) { // we intentionally throw a NPE and catch it to see whether it is logged by GW.
            expectedExceptionTypeCaught = true;
        }
        assertTrue("Expected error did not occur", expectedExceptionTypeCaught);

        // pop the unused exitingMessage triggered by "dereferenceNull" call
        inMemoryTracer.popMessage();

        Message<?> onErrorMessage = inMemoryTracer.popMessage();
        // verify message type
        assertTrue("Invalid message type: " + onErrorMessage.getClass(), onErrorMessage instanceof OnErrorMessage);

        Object payload = onErrorMessage.getPayload();
        // verify that payload has the expected type
        assertTrue("Payload has an unexpected type: " + payload.getClass(), payload instanceof NullPointerException);
    }

    public String dereferenceNull() {
        String myNull = null;
        return myNull.toUpperCase();
    }

    // Issue 48: verify that deeply nested classes (OnErrorTest.SomeClass.CASE_A anonymous class) are not getting instrumented multiple times. Otherwise it will cause
    // a compilation error, since $throwable_ will be declared multiple times.
    enum SomeClass {
        CASE_A {
            @Override
            int someMethod() {
                // trigger NumberFormatException
                return Integer.valueOf(null);
            }
        },
        CASE_B {
            @Override
            int someMethod() {
                throw new UnsupportedOperationException("This is a test!");
            }
        };

        abstract int someMethod();
    }

    @Test
    public void testEnumBasedAnonymousClassSupport() {
        InMemoryTracer inMemoryTracer = fetchedPreparedInMemoryTracer();
        boolean expectedExceptionTypeCaught = false;
        try {
            SomeClass.CASE_A.someMethod();
        }
        catch (NumberFormatException e) { // we intentionally throw a NPE and catch it to see whether it is logged by GW.
            expectedExceptionTypeCaught = true;
        }
        assertTrue("Expected error did not occur", expectedExceptionTypeCaught);

        // pop the unused exitingMessage
        inMemoryTracer.popMessage();

        Message<?> onErrorMessage = inMemoryTracer.popMessage();
        // verify message type
        assertTrue("Invalid message type: " + onErrorMessage.getClass(), onErrorMessage instanceof OnErrorMessage);

        Object payload = onErrorMessage.getPayload();
        // verify that payload has the expected type
        assertTrue("Payload has an unexpected type: " + payload.getClass(), payload instanceof NumberFormatException);

        // verify that the other implementation has correct error message support

        inMemoryTracer.clearMessages();
        expectedExceptionTypeCaught = false;
        try {
            SomeClass.CASE_B.someMethod();
        }
        catch (UnsupportedOperationException e) { // we intentionally throw a NPE and catch it to see whether it is logged by GW.
            expectedExceptionTypeCaught = true;
        }

        assertTrue("Expected error did not occur", expectedExceptionTypeCaught);

        // pop the unused exitingMessage
        inMemoryTracer.popMessage();

        onErrorMessage = inMemoryTracer.popMessage();
        // verify message type
        assertTrue("Invalid message type: " + onErrorMessage.getClass(), onErrorMessage instanceof OnErrorMessage);

        payload = onErrorMessage.getPayload();
        // verify that payload has the expected type
        assertTrue("Payload has an unexpected type: " + payload.getClass(), payload instanceof UnsupportedOperationException);
    }

}
