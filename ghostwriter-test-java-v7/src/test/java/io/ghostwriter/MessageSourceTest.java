package io.ghostwriter;


import io.ghostwriter.message.EnteringMessage;
import io.ghostwriter.message.ExitingMessage;
import io.ghostwriter.message.Message;
import io.ghostwriter.message.ReturningMessage;
import io.ghostwriter.test.TestBase;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class MessageSourceTest extends TestBase {
    
    static class SomeTestClass {
        
        public static int someTestMethod(int a) {
            return a + a + 1;            
        }
        
    }

    @Test
    public void testInnerClassStaticMethodSource() {
        InMemoryTracer inMemoryTracer = fetchedPreparedInMemoryTracer();
        SomeTestClass.someTestMethod(42);
        
        // pop the exitingMessage
        Message<?> exitingMessage = inMemoryTracer.popMessage();
        boolean isExitingMessage = exitingMessage instanceof ExitingMessage;
        // test whether it has the correct type
        assertTrue("Expected message type " + ExitingMessage.class.getSimpleName() + "got: " + exitingMessage.getClass().getSimpleName(), isExitingMessage);
        
        ExitingMessage exitingMsg = (ExitingMessage) exitingMessage;
        // test whether it has the correct source, which should be SomeTestClass itself.
        Object source = exitingMsg.getSource();
        assertTrue("Expected type of Class<SomeTestClass> got: " + source.getClass().getSimpleName(), source instanceof Class<?>);
        Class<SomeTestClass> someTestClassSource = (Class<SomeTestClass>) source;
        boolean hasCorrectClassType = SomeTestClass.class.getCanonicalName().equals(someTestClassSource.getCanonicalName());
        assertTrue("Expected message source " + SomeTestClass.class.getCanonicalName() + "got: " + someTestClassSource.getCanonicalName(), hasCorrectClassType);

        // pop the returning message
        Message<?> returningMessage = inMemoryTracer.popMessage();
        boolean isReturningMessage = returningMessage instanceof ReturningMessage;
        assertTrue("Expected message type " + ReturningMessage.class.getSimpleName() + "got: " + returningMessage.getClass().getSimpleName(), isReturningMessage);

        // pop the entering message
        Message<?> enteringMessage = inMemoryTracer.popMessage();
        boolean isEnteringMessage = enteringMessage instanceof EnteringMessage;
        // test whether it has the correct type
        assertTrue("Expected message type " + EnteringMessage.class.getSimpleName() + "got: " + enteringMessage.getClass().getSimpleName(), isEnteringMessage);

        EnteringMessage enteringMsg = (EnteringMessage) enteringMessage;
        // test whether it has the correct source, which should be SomeTestClass itself.
        source = enteringMsg.getSource();
        assertTrue("Expected type of Class<SomeTestClass> got: " + source.getClass().getSimpleName(), source instanceof Class<?>);
        someTestClassSource = (Class<SomeTestClass>) source;
        hasCorrectClassType = SomeTestClass.class.getCanonicalName().equals(someTestClassSource.getCanonicalName());
        assertTrue("Expected message source " + SomeTestClass.class.getCanonicalName() + "got: " + someTestClassSource.getCanonicalName(), hasCorrectClassType);
    }

    @Test
    public void testMemberMethodSource() {
        InMemoryTracer inMemoryTracer = fetchedPreparedInMemoryTracer();
        someMemberMethod();

        // pop the exitingMessage
        Message<?> exitingMessage = inMemoryTracer.popMessage();
        boolean isExitingMessage = exitingMessage instanceof ExitingMessage;
        // test whether it has the correct type
        assertTrue("Expected message type " + ExitingMessage.class.getSimpleName() + "got: " + exitingMessage.getClass().getSimpleName(), isExitingMessage);

        ExitingMessage exitingMsg = (ExitingMessage) exitingMessage;
        // test whether it has the correct source, which should be SomeTestClass itself (this).
        Object source = exitingMsg.getSource();
        assertTrue("Expected type of MessageSourceTest got: " + source.getClass().getSimpleName(), source instanceof MessageSourceTest);
        assertTrue("Expected instance was 'this' got: " + source, this.equals(source));

        // pop the returning message
        Message<?> returningMessage = inMemoryTracer.popMessage();
        boolean isReturningMessage = returningMessage instanceof ReturningMessage;
        assertTrue("Expected message type " + ReturningMessage.class.getSimpleName() + "got: " + returningMessage.getClass().getSimpleName(), isReturningMessage);

        // pop the entering message
        Message<?> enteringMessage = inMemoryTracer.popMessage();
        boolean isEnteringMessage = enteringMessage instanceof EnteringMessage;
        // test whether it has the correct type
        assertTrue("Expected message type " + EnteringMessage.class.getSimpleName() + "got: " + enteringMessage.getClass().getSimpleName(), isEnteringMessage);

        EnteringMessage enteringMsg = (EnteringMessage) enteringMessage;
        // test whether it has the correct source, which should be SomeTestClass itself (this).
        source = enteringMsg.getSource();
        assertTrue("Expected type of SomeTestClass got: " + source.getClass().getSimpleName(), source instanceof MessageSourceTest);
        assertTrue("Expected instance was 'this' got: " + source, this.equals(source));
    }
    
    private int someMemberMethod() {
        return 314;
    }
    
}
