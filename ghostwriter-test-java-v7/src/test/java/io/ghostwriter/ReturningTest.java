package io.ghostwriter;

import io.ghostwriter.test.MessageSequenceAsserter;
import io.ghostwriter.test.TestBase;
import org.junit.Test;

import java.util.Collections;
import java.util.List;


public class ReturningTest extends TestBase {

    public void methodWithEmptyReturn() {
        return;
    }

    @Test
    public void testEmptyReturn() {
        final InMemoryTracer inMemoryTracer = fetchedPreparedInMemoryTracer();
        inMemoryTracer.disableEnteringExitingTracking();
        methodWithEmptyReturn();
        MessageSequenceAsserter.messageSequence().empty();
    }

    public int returnSomeNumber() {
        return 756;
    }

    @Test
    public void testReturnPrimitiveCapture() {
        final InMemoryTracer inMemoryTracer = fetchedPreparedInMemoryTracer();
        inMemoryTracer.disableEnteringExitingTracking();
        inMemoryTracer.clearMessages();
        returnSomeNumber();
        MessageSequenceAsserter.messageSequence()
                .returning("returnSomeNumber", 756)
                .empty();
    }

    // covers issue #75, issue brought up by using generics instead of plain Object in the in-place returning expression
    public <T> List<List<T>> returnAmbiguousType() {
        // In the GW returning API call, the type is inferred from the return value.
        // From the Collections.emptyList() that would be List<Object>, which leads to compilation errors.
        return Collections.emptyList();
    }

    @Test
    public void testReturnAmbiguousType() {
        final InMemoryTracer inMemoryTracer = fetchedPreparedInMemoryTracer();
        inMemoryTracer.disableEnteringExitingTracking();
        inMemoryTracer.clearMessages();
        returnAmbiguousType();
        MessageSequenceAsserter.messageSequence()
                .returning("returnAmbiguousType", Collections.emptyList())
                .empty();
    }

}
