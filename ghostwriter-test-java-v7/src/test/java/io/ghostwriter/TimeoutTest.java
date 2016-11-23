package io.ghostwriter;

import io.ghostwriter.annotation.Timeout;
import io.ghostwriter.test.MessageSequenceAsserter;
import io.ghostwriter.test.TestBase;
import org.junit.Test;


public class TimeoutTest extends TestBase {

    @Timeout(threshold = 10)
    public void timeSensitiveMethod() {
        try {
            Thread.sleep(25);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e); // this will cause the test to fail, so we achieve the desired goal
        }
    }

    @io.ghostwriter.annotation.Timeout(threshold = 5)
    public void anotherTimeSensitiveMethod() {
        try {
            Thread.sleep(25);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e); // this will cause the test to fail, so we achieve the desired goal
        }
    }

    @Timeout(threshold = 1000)
    public void underThreshold() {
        // do nothing
    }

    @Test
    public void testTimeoutEvent() {
        fetchedPreparedInMemoryTracer();
        timeSensitiveMethod();
        MessageSequenceAsserter.messageSequence()
                .entering("timeSensitiveMethod")
                .timeout("timeSensitiveMethod", 10)
                .exiting("timeSensitiveMethod");
    }

    @Test
    public void testFullyQualifiedTimeoutAnnotationName() {
        fetchedPreparedInMemoryTracer();
        anotherTimeSensitiveMethod();
        MessageSequenceAsserter.messageSequence()
                .entering("anotherTimeSensitiveMethod")
                .timeout("anotherTimeSensitiveMethod", 5)
                .exiting("anotherTimeSensitiveMethod");
    }

    @Test
    public void testAnnotatedUnderThreshold() {
        fetchedPreparedInMemoryTracer();
        underThreshold();
        MessageSequenceAsserter.messageSequence()
                .entering("underThreshold")
                // assert that no timeout message is triggered, even though the method is instrumented for it
                .exiting("underThreshold");
    }

}
