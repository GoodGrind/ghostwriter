package io.ghostwriter;

import io.ghostwriter.test.MessageSequenceAsserter;
import io.ghostwriter.test.Parameter;
import io.ghostwriter.test.TestBase;
import org.junit.Test;

/**
 * This test assures that even after instrumentation the first call of the instrumented 
 * constructor body is the original this()/super() call.
 */
public class ConstructorTest extends TestBase {

    private static final String CONSTRUCTOR_NAME = "<init>";

    // here we also test nested class declaration support
    class ConstructorStuff {

        final private int myValue;

        public ConstructorStuff() {
            // it is intentional that we do not use 'this(444)' here
            myValue = 444; // this will trigger a value change event besides the entering-exiting ones
            assert myValue != 0; // avoid the not used warning, we use it during test execution through GW
        }

        public ConstructorStuff(int myValue) {
            this(myValue, 1);
        }

        public ConstructorStuff(int myValue, int multiplier) {
            this.myValue = myValue * multiplier;
        }

    }

    class ConstructorStuffTake2 extends ConstructorStuff {

        final private float otherValue;

        public ConstructorStuffTake2() {
            super(414);
            otherValue = 3.14F;
            assert  otherValue != 0.0F; // avoid the not used warning, it is used through GW during test execution
        }

    }

    @Test
    public void testNoArgConstructor() {
        // entering the test case also generates messages, we drop those. This way the test work whether we apply @Exclude or not
        InMemoryTracerProvider.INSTANCE.getTracer().clearMessages();
        new ConstructorStuff();

        MessageSequenceAsserter.messageSequence()
                .entering(CONSTRUCTOR_NAME)
                .exiting(CONSTRUCTOR_NAME);
    }

    @Test
    public void testOneArgConstructorWithThisCall() {
        // entering the test case also generates messages, we drop those. This way the test work whether we apply @Exclude or not
        fetchedPreparedInMemoryTracer();
        new ConstructorStuff(666);

        MessageSequenceAsserter.messageSequence()
                // remember, this(...) must be the 1st statement in a constructor, hence the nested this() call is  traced 1st
                .entering(CONSTRUCTOR_NAME, new Parameter<>("myValue", 666), new Parameter<>("multiplier", 1)) // this(myValue, 1)
                .exiting(CONSTRUCTOR_NAME)
                .entering(CONSTRUCTOR_NAME, new Parameter<>("myValue", 666)) //    new ConstructorStuff(666);
                .exiting(CONSTRUCTOR_NAME);
    }

    @Test
    public void testNoArgConstructorWithSuperCall() {
        fetchedPreparedInMemoryTracer();
        new ConstructorStuffTake2();

        // note, since super(), this() must be the 1st statements in a constructor call, the message order differs than in case of regular methods
        MessageSequenceAsserter.messageSequence()
                //this(myValue, 1);
                .entering(CONSTRUCTOR_NAME, new Parameter<>("myValue", 414), new Parameter<>("multiplier", 1))
                .exiting(CONSTRUCTOR_NAME)
                // super(414);
                .entering(CONSTRUCTOR_NAME, new Parameter<>("myValue", 414))
                .exiting(CONSTRUCTOR_NAME)
                // new ConstructorStuffTake2();
                .entering(CONSTRUCTOR_NAME)
                .exiting(CONSTRUCTOR_NAME);
    }
    
}
