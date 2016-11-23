package io.ghostwriter;

import io.ghostwriter.annotation.Exclude;
import io.ghostwriter.message.EnteringMessage;
import io.ghostwriter.message.Message;
import io.ghostwriter.test.MessageSequenceAsserter;
import io.ghostwriter.test.TestBase;
import org.junit.Assert;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;


public class ExcludeAnnotationTest extends TestBase {

    @Exclude
    public class InnerExcludedClass {

        public int someRandomValue() {
            return 24;
        }

    }

    public class InnerClassWithExcludedConstructor {

        private final double someValue;

        @Exclude
        public InnerClassWithExcludedConstructor(double someValue) {
            this.someValue = someValue;
            assert this.someValue != Double.NaN; // in order to avoid the not used warning...
        }

    }

    @Test
    public void testExcludedTopLevelClass() {
        final InMemoryTracer inMemoryTracer = fetchedPreparedInMemoryTracer();
        inMemoryTracer.clearMessages();
        ExcludedTopLevelClass someVal = new ExcludedTopLevelClass();
        someVal.meaningOfLife();
        final int numMessages = inMemoryTracer.numberOfMessages();
        assertTrue("Excluded class should not produce GW events! Got: " + numMessages, numMessages == 0);
    }

    @Test
    public void testExcludedInnerClass() {
        final InMemoryTracer inMemoryTracer = fetchedPreparedInMemoryTracer();
        inMemoryTracer.clearMessages();
        InnerExcludedClass someVal = new InnerExcludedClass();
        someVal.someRandomValue();
        final int numMessages = inMemoryTracer.numberOfMessages();
        assertTrue("Excluded class should not produce GW events! Got: " + numMessages, numMessages == 0);
    }

    @Test
    public void testExcludedConstructor() {
        final InMemoryTracer inMemoryTracer = fetchedPreparedInMemoryTracer();
        inMemoryTracer.clearMessages();
        new InnerClassWithExcludedConstructor(3.14);
        final int numMessages = inMemoryTracer.numberOfMessages();
        assertTrue("Excluded constructor should not produce GW events! Got: " + numMessages, numMessages == 0);
    }

    @Test
    public void testExcludedMethod() {
        final InMemoryTracer inMemoryTracer = fetchedPreparedInMemoryTracer();
        inMemoryTracer.clearMessages();
        excludedMethod();
        final int numberOfMessages = inMemoryTracer.numberOfMessages();
        assertTrue("Excluded method should not produce GW events!", numberOfMessages == 0);
    }

    @Exclude // the annotation signals the GW instrumenter to ignore this method
    public int excludedMethod() {
        int i = 3;
        int j = i + 42;
        for (int x = 0; x < j; ++x) {
            i += x;
        }

        return i;
    }

    @Test
    public void testExcludeMethodParameter() {
        final InMemoryTracer inMemoryTracer = fetchedPreparedInMemoryTracer();
        inMemoryTracer.clearMessages();
        excludeMethodParameter(1, 2, 3);

        // pop exiting message, we don't need that now
        inMemoryTracer.popMessage();

        // pop returning message, we don't need that now
        inMemoryTracer.popMessage();

        Message<?> enteringMessage = inMemoryTracer.popMessage();
        boolean isEnteringMessage = enteringMessage instanceof EnteringMessage;
        Assert.assertTrue("Invalid message type: " + enteringMessage.getClass(), isEnteringMessage);

        EnteringMessage msg = (EnteringMessage) enteringMessage;
        boolean hasParameters = msg.hasParameters();
        Assert.assertTrue("Parameter information not traced", hasParameters);

        EnteringMessage.BasePayload payload = msg.getPayload();
        Object[] parameters = payload.getParameters();
        final int numberOfMethodParameters = 3;
        final int numberOfExcludedParameters = 1;
        final int expectedNumberOfItemsInPayload = (numberOfMethodParameters - numberOfExcludedParameters) * NUMBER_OF_ENTRIES_PER_TRACED_PARAMETER;
        boolean hasCorrectNumberOfParameters = parameters.length ==  expectedNumberOfItemsInPayload;
        Assert.assertTrue("Missing or excess parameter tracking. Number of parameters: " + parameters.length / NUMBER_OF_ENTRIES_PER_TRACED_PARAMETER, hasCorrectNumberOfParameters);

        // first parameter check

        final int EXPECTED_INDEX_OF_FIRST_PARAMETER_NAME = 0;
        Object aParameterName = parameters[EXPECTED_INDEX_OF_FIRST_PARAMETER_NAME];
        boolean doesFirstEntryContainTheName = aParameterName instanceof String;
        Assert.assertTrue("Incorrect entry order. Parameter name has type of: " + aParameterName.getClass(), doesFirstEntryContainTheName);

        boolean isCorrectNameExtracted = "a".equals(aParameterName);
        Assert.assertTrue("Parameter name does not match the expected 'a'! Got: " + aParameterName, isCorrectNameExtracted);

        final int EXPECTED_INDEX_OF_FIRST_PARAMETER_VALUE = 1;
        Object aParameterValue = parameters[EXPECTED_INDEX_OF_FIRST_PARAMETER_VALUE];
        boolean doesTracedValueHaveCorrectType = aParameterValue instanceof Integer;
        Assert.assertTrue("Parameter value does not have the expected type! Got: " + aParameterValue.getClass(), doesTracedValueHaveCorrectType);

        int tracedValue = (int) aParameterValue;
        boolean doesHaveCorrectValueTraced = tracedValue == 1;
        Assert.assertTrue("Parameter value does not match the expected value '1'! Got: " + tracedValue, doesHaveCorrectValueTraced);

        // second parameter checks

        final int EXPECTED_INDEX_OF_SECOND_PARAMETER_NAME = 2;
        Object cParameterName = parameters[EXPECTED_INDEX_OF_SECOND_PARAMETER_NAME];
        doesFirstEntryContainTheName = cParameterName instanceof String;
        Assert.assertTrue("Incorrect entry order. Parameter name has type of: " + cParameterName.getClass(), doesFirstEntryContainTheName);

        isCorrectNameExtracted = "c".equals(cParameterName);
        Assert.assertTrue("Parameter name does not match the expected 'c'! Got: " + cParameterName, isCorrectNameExtracted);

        final int EXPECTED_INDEX_OF_SECOND_PARAMETER_VALUE = 3;
        Object cParameterValue = parameters[EXPECTED_INDEX_OF_SECOND_PARAMETER_VALUE];
        doesTracedValueHaveCorrectType = cParameterValue instanceof Integer;
        Assert.assertTrue("Parameter value does not have the expected type! Got: " + aParameterValue.getClass(), doesTracedValueHaveCorrectType);

        tracedValue = (int) cParameterValue;
        doesHaveCorrectValueTraced = tracedValue == 3;
        Assert.assertTrue("Parameter value does not match the expected value '3'! Got: " + tracedValue, doesHaveCorrectValueTraced);
    }

    public int excludeMethodParameter(int a, @Exclude int someExcludedParameter, int c) {
        return a + someExcludedParameter + c;
    }

    @Test
    public void testMethodWithExcludedLocalVariables() {
        fetchedPreparedInMemoryTracer();
        enableValueChangeTracking();
        methodWithExcludedLocalVariable();
        disableValueChangeTracking();

        MessageSequenceAsserter.messageSequence()
                .entering("methodWithExcludedLocalVariable")
                .valueChange("methodWithExcludedLocalVariable", "a", Integer.valueOf(3))
                .returning("methodWithExcludedLocalVariable", Integer.valueOf(5))
                .exiting("methodWithExcludedLocalVariable");
    }

    public int methodWithExcludedLocalVariable() {
        final int a = 3;
        @Exclude int someVariable = 2;
        return a + someVariable;
    }

    @Test
    public void testFullyQualifiedExcludeUsage() {
        fetchedPreparedInMemoryTracer();
        enableValueChangeTracking();
        localVariableWithFullyQualifiedExclude();
        disableValueChangeTracking();

        MessageSequenceAsserter.messageSequence()
                .entering("localVariableWithFullyQualifiedExclude")
                .valueChange("localVariableWithFullyQualifiedExclude", "a", Integer.valueOf(322))
                .returning("localVariableWithFullyQualifiedExclude", Integer.valueOf(324))
                .exiting("localVariableWithFullyQualifiedExclude");
    }

    public int localVariableWithFullyQualifiedExclude() {
        final int a = 322;
        @io.ghostwriter.annotation.Exclude int fqnVariable = 2;
        return a + fqnVariable;
    }

}
