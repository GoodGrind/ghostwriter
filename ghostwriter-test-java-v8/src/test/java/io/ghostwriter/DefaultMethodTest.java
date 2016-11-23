package io.ghostwriter;


import io.ghostwriter.test.MessageSequenceAsserter;
import io.ghostwriter.test.Parameter;
import io.ghostwriter.test.TestBase;
import org.junit.Test;

public class DefaultMethodTest extends TestBase {

    interface SomeInterface {
        void doSomething();
        default String doDefaultSomething(int val) {
            int incVal = val + 1;
            String strVal = String.valueOf(incVal);
            return strVal;
        }
    }

    class SomeClass implements SomeInterface {
        @Override
        public void doSomething() {
            doDefaultSomething(443);
        }
    }

    @Test
    public void testDefaultMethod() {
        final InMemoryTracer inMemoryTracer = fetchedPreparedInMemoryTracer();
        SomeClass someClass = new SomeClass();
        inMemoryTracer.clearMessages(); // clear SomeClass constructor calls.
        inMemoryTracer.enableValueChangeTracking();
        someClass.doSomething();
        MessageSequenceAsserter.messageSequence()
                .entering("doSomething")
                .entering("doDefaultSomething", new Parameter<>("val", 443))
                .valueChange("doDefaultSomething", "incVal", 444)
                .valueChange("doDefaultSomething", "strVal", "444")
                .returning("doDefaultSomething", "444")
                .exiting("doDefaultSomething")
                .exiting("doSomething");
    }

}
