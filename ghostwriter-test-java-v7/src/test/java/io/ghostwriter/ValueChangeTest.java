package io.ghostwriter;

import io.ghostwriter.test.MessageSequenceAsserter;
import io.ghostwriter.test.Parameter;
import io.ghostwriter.test.TestBase;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class ValueChangeTest extends TestBase {

    @Test
    public void testSingleValueAssignment() {
        InMemoryTracer inMemoryTracer = fetchedPreparedInMemoryTracer();
        inMemoryTracer.enableValueChangeTracking();
        singleValueAssignment();
        inMemoryTracer.disableValueChangeTracking();

        MessageSequenceAsserter.messageSequence()
                .entering("singleValueAssignment")
                .valueChange("singleValueAssignment", "a", 42)
                .valueChange("singleValueAssignment", "a", 314)
                .returning("singleValueAssignment", 314)
                .exiting("singleValueAssignment");
    }

    public int singleValueAssignment() {
        int a = 42; // Java lang treats this as initialization and not assignment
        a = 314; // this is an assignment
        return a;
    }


    @Test
    public void testPrefixAndPostfixOperatorBasedValueChanges() {
        InMemoryTracer inMemoryTracer = fetchedPreparedInMemoryTracer();
        inMemoryTracer.enableValueChangeTracking();
        prefixAndPostfixOperatorBasedValueChanges();
        inMemoryTracer.disableValueChangeTracking();

        MessageSequenceAsserter.messageSequence()
                .entering("prefixAndPostfixOperatorBasedValueChanges")
                .valueChange("prefixAndPostfixOperatorBasedValueChanges", "i", 7)
                .valueChange("prefixAndPostfixOperatorBasedValueChanges", "i", 8)
                .valueChange("prefixAndPostfixOperatorBasedValueChanges", "i", 9)
                .valueChange("prefixAndPostfixOperatorBasedValueChanges", "i", 8)
                .valueChange("prefixAndPostfixOperatorBasedValueChanges", "i", 7)
                .returning("prefixAndPostfixOperatorBasedValueChanges", 7)
                .exiting("prefixAndPostfixOperatorBasedValueChanges");
    }

    public int prefixAndPostfixOperatorBasedValueChanges() {
        int i = 7;

        ++i;
        i++;
        --i;
        i--;

        return i;
    }


    @Test
    public void testForIterationWithBody() {
        InMemoryTracer inMemoryTracer = fetchedPreparedInMemoryTracer();
        inMemoryTracer.enableValueChangeTracking();
        forIterationWithBody();
        inMemoryTracer.disableValueChangeTracking();

        MessageSequenceAsserter.messageSequence()
                .entering("forIterationWithBody")
                .valueChange("forIterationWithBody", "sum", 0)
                .valueChange("forIterationWithBody", "i", 0)
                .valueChange("forIterationWithBody", "sum", 0)
                .valueChange("forIterationWithBody", "i", 1)
                .valueChange("forIterationWithBody", "sum", 1)
                .returning("forIterationWithBody", 1)
                .exiting("forIterationWithBody");
    }

    public int forIterationWithBody() {
        int sum = 0; // Again... this is an initialization and not an assignment! Don't forget! GW has to support both!
        for (int i = 0; i < 2; ++i) {
            sum += i;
        }

        return sum;
    }

    @Test
    public void testForIterationWithoutBody() {
        InMemoryTracer inMemoryTracer = fetchedPreparedInMemoryTracer();
        inMemoryTracer.enableValueChangeTracking();
        forIterationWithoutBody();
        inMemoryTracer.disableValueChangeTracking();

        MessageSequenceAsserter.messageSequence()
                .entering("forIterationWithoutBody")
                .valueChange("forIterationWithoutBody", "acc", 0)
                .valueChange("forIterationWithoutBody", "i", 0)
                .valueChange("forIterationWithoutBody", "acc", 0)
                .valueChange("forIterationWithoutBody", "i", 1)
                .valueChange("forIterationWithoutBody", "acc", 0)
                // NOTE(snorbi07): we don't capture the last incrementation of 'i' since captures are triggered inside the iteration block
                //.valueChange("forIterationWithoutBody", "i", 2)
                .returning("forIterationWithoutBody", 0)
                .exiting("forIterationWithoutBody");
    }

    public int forIterationWithoutBody() {
        int acc = 0;
        for (int i = 0; i < 2; ++i)
            acc *= i;

        return acc;
    }

    @Test
    public void testForEachIteration() {
        InMemoryTracer inMemoryTracer = fetchedPreparedInMemoryTracer();
        inMemoryTracer.enableValueChangeTracking();
        forEachIteration();
        inMemoryTracer.disableValueChangeTracking();

        List<Double> expectedListValue = new ArrayList<>();
        expectedListValue.add(1.4);
        expectedListValue.add(2.3);

        MessageSequenceAsserter.messageSequence()
                .entering("forEachIteration")
                .valueChange("forEachIteration", "myList", expectedListValue)
                .valueChange("forEachIteration", "sum", 0.0)
                .valueChange("forEachIteration", "item", 1.4)
                .valueChange("forEachIteration", "sum", 0.0)
                .valueChange("forEachIteration", "item", 2.3)
                .valueChange("forEachIteration", "sum", 0.0)
                .returning("forEachIteration", 0.0)
                .exiting("forEachIteration");
    }

    public double forEachIteration() {
        List<Double> myList = new ArrayList<>();
        myList.add(1.4);
        myList.add(2.3);

        Double sum = 0.0;
        for (Double item : myList) {
            sum *= item;
        }

        return sum;
    }

    @Test
    public void testChainAssignment() {
        InMemoryTracer inMemoryTracer = fetchedPreparedInMemoryTracer();
        inMemoryTracer.enableValueChangeTracking();
        chainAssignment();
        inMemoryTracer.disableValueChangeTracking();

        MessageSequenceAsserter.messageSequence()
                .entering("chainAssignment")
                .valueChange("chainAssignment", "a", 3)
                .valueChange("chainAssignment", "b", 4)
                .valueChange("chainAssignment", "c", 0)
                .valueChange("chainAssignment", "b", 2)
                .valueChange("chainAssignment", "b", 0)
                .valueChange("chainAssignment", "a", 0)
                .returning("chainAssignment", 0)
                .exiting("chainAssignment");
    }

    public int chainAssignment() {
        int a = 3, b = 4, c = 0;
        b = c + 2;
        a = b = c;
        return c + a;
    }

    @Test
    public void testUninitializedVariableCapture() {
        InMemoryTracer inMemoryTracer = fetchedPreparedInMemoryTracer();
        inMemoryTracer.enableValueChangeTracking();
        uninitializedVariableCapture();
        inMemoryTracer.disableValueChangeTracking();

        /*
                int z;
        boolean someCondition = true;
        if (someCondition) {
            z = 42;
        } else {
            z = 52;
        }

        return z;
         */

        MessageSequenceAsserter.messageSequence()
                .entering("uninitializedVariableCapture")
                .valueChange("uninitializedVariableCapture", "someCondition", true)
                .valueChange("uninitializedVariableCapture", "z", 42)
                .returning("uninitializedVariableCapture", 42)
                .exiting("uninitializedVariableCapture");
    }

    public int uninitializedVariableCapture() {
        int z;
        boolean someCondition = true;
        if (someCondition) {
            z = 42;
        } else {
            z = 52;
        }

        return z;
    }

    @Test
    public void testBlocklessControlFlowConstruct() {
        InMemoryTracer inMemoryTracer = fetchedPreparedInMemoryTracer();
        inMemoryTracer.enableValueChangeTracking();
        blocklessControlFlowConstruct();
        inMemoryTracer.disableValueChangeTracking();

        MessageSequenceAsserter.messageSequence()
                .entering("blocklessControlFlowConstruct")
                .valueChange("blocklessControlFlowConstruct", "something", true)
                .valueChange("blocklessControlFlowConstruct", "a", 0)
                .valueChange("blocklessControlFlowConstruct", "a", 2)
                .returning("blocklessControlFlowConstruct", 2)
                .exiting("blocklessControlFlowConstruct");
    }

    public int blocklessControlFlowConstruct() {
        boolean something = true;
        int a = 0;
        if (something)
            a = 2;
        else
            a = 3;

        return a;
    }

    @Test
    public void testEnclosingScopeVariableMutationInForStepSection() {
        InMemoryTracer inMemoryTracer = fetchedPreparedInMemoryTracer();
        inMemoryTracer.enableValueChangeTracking();
        enclosingScopeVariableMutationInForStepSection();
        inMemoryTracer.disableValueChangeTracking();

        MessageSequenceAsserter.messageSequence()
                .entering("enclosingScopeVariableMutationInForStepSection")
                .valueChange("enclosingScopeVariableMutationInForStepSection", "i", 0)
                .valueChange("enclosingScopeVariableMutationInForStepSection", "j", 7)
                // NOTE(snorbi07): 1st trace of variables refers to the initialized value,
                // fixed would be to use inline capturing of values instead of the iteration body
                .valueChange("enclosingScopeVariableMutationInForStepSection", "j", 7)
                .valueChange("enclosingScopeVariableMutationInForStepSection", "i", 0)
                // END-NOTE
                .valueChange("enclosingScopeVariableMutationInForStepSection", "j", 6)
                .valueChange("enclosingScopeVariableMutationInForStepSection", "i", 1)
                .valueChange("enclosingScopeVariableMutationInForStepSection", "j", 5)
                .valueChange("enclosingScopeVariableMutationInForStepSection", "i", 2)
                .returning("enclosingScopeVariableMutationInForStepSection", 7)
                .exiting("enclosingScopeVariableMutationInForStepSection");
    }

    public int enclosingScopeVariableMutationInForStepSection() {
        int i = 0, j = 7;

        // The important part here is that we do not use the initialization section of the for loop to declare i and j
        // however we change them in the 'step' section.
        for ( ; i < 3; i++, --j);

        return i + j;
    }

    // Issue 49: assigning a value to an array causes compilation failure.
    // Invalid AST was generated for reading indexed array values.
    @Test
    public void testArrayIndexBasedAssignment() {
        int[] passedArray = new int[3];
        InMemoryTracer inMemoryTracer = fetchedPreparedInMemoryTracer();
        inMemoryTracer.enableValueChangeTracking();
        arrayIndexBasedAssignment(passedArray);
        inMemoryTracer.disableValueChangeTracking();

        MessageSequenceAsserter.messageSequence()
                .entering("arrayIndexBasedAssignment", new Parameter<>("myArray", passedArray))
                .valueChange("arrayIndexBasedAssignment", "myArray[0]", 1)
                .exiting("arrayIndexBasedAssignment");
    }

    public void arrayIndexBasedAssignment(int[] myArray) {
        myArray[0] = 1;
    }

    @Test
    public void testMethodWithSwitchCaseConstruct() {
        InMemoryTracer inMemoryTracer = fetchedPreparedInMemoryTracer();
        inMemoryTracer.enableValueChangeTracking();
        methodWithSwitchCaseConstruct(SomeSwitchStates.ALMOST_PI, 1);
        methodWithSwitchCaseConstruct(SomeSwitchStates.ZEROS, 2);
        methodWithSwitchCaseConstruct(SomeSwitchStates.MEANING_OF_LIFE, 3);
        inMemoryTracer.disableValueChangeTracking();

        MessageSequenceAsserter.messageSequence()
                // SomeSwitchStates constructors
                .entering("<init>")
                .exiting("<init>")
                .entering("<init>")
                .exiting("<init>")
                .entering("<init>")
                .exiting("<init>")
                // methodWithSwitchCaseConstruct(SomeSwitchStates.ALMOST_PI, 1);
                .entering("methodWithSwitchCaseConstruct", new Parameter<>("state", SomeSwitchStates.ALMOST_PI), new Parameter<>("value", 1))
                .valueChange("methodWithSwitchCaseConstruct", "result", -1)
                .returning("methodWithSwitchCaseConstruct", 314)
                .exiting("methodWithSwitchCaseConstruct")
                // methodWithSwitchCaseConstruct(SomeSwitchStates.ZEROS, 2);
                .entering("methodWithSwitchCaseConstruct", new Parameter<>("state", SomeSwitchStates.ZEROS), new Parameter<>("value", 2))
                .valueChange("methodWithSwitchCaseConstruct", "result", -1)
                .valueChange("methodWithSwitchCaseConstruct", "leadingZeros", 30)
                .valueChange("methodWithSwitchCaseConstruct", "result", 30)
                .valueChange("methodWithSwitchCaseConstruct", "enteredDefault", true)
                .returning("methodWithSwitchCaseConstruct", 30)
                .exiting("methodWithSwitchCaseConstruct")
                // methodWithSwitchCaseConstruct(SomeSwitchStates.MEANING_OF_LIFE, 3);
                .entering("methodWithSwitchCaseConstruct", new Parameter<>("state", SomeSwitchStates.MEANING_OF_LIFE), new Parameter<>("value", 3))
                .valueChange("methodWithSwitchCaseConstruct", "result", -1)
                .valueChange("methodWithSwitchCaseConstruct", "result", 42)
                .returning("methodWithSwitchCaseConstruct", 42)
                .exiting("methodWithSwitchCaseConstruct");
    }

    private enum SomeSwitchStates {
        MEANING_OF_LIFE,
        ALMOST_PI,
        ZEROS
    }

    public int methodWithSwitchCaseConstruct(SomeSwitchStates state, int value) {
        int result = -1;

        switch (state) {
            case MEANING_OF_LIFE:
                result = 42;
                break; // test break based code

            case ALMOST_PI:
                return 314; // test immediate return

            case ZEROS:
                int leadingZeros = Integer.numberOfLeadingZeros(value); // test case based local variable support
                result = leadingZeros;
                // test fallthrough

            default:
                // we check for this assignment in the instrumented code
                boolean enteredDefault = true; // test if 'default' case is supported
                assert enteredDefault; // to avoid unused code issues
                if (result == -1) {
                    throw new AssertionError("Something went utterly wrong while testing the fallthrough case");
                }
        }

        return result;
    }

    @Test
    public void testMethodWithBlocklessMutationInReturnExpression() {
        fetchedPreparedInMemoryTracer();
        enableValueChangeTracking();
        methodWithBlocklessMutationInReturnExpression(1);
        disableValueChangeTracking();
        MessageSequenceAsserter.messageSequence()
                .entering("methodWithBlocklessMutationInReturnExpression", new Parameter<>("i", 1))
                .valueChange("methodWithBlocklessMutationInReturnExpression", "x", 0)
                .valueChange("methodWithBlocklessMutationInReturnExpression", "x", 1)
                .returning("methodWithBlocklessMutationInReturnExpression", 1)
                .exiting("methodWithBlocklessMutationInReturnExpression");
    }

    public int methodWithBlocklessMutationInReturnExpression(int i) {
        int x = 0;
        if (i == 1) {
            return ++x;
        }
        else
            return --x;
    }

    @Test
    public void testMethodWithAssignmentInReturnExpression() {
        fetchedPreparedInMemoryTracer();
        enableValueChangeTracking();
        methodWithAssignmentInReturnExpression(true);
        disableValueChangeTracking();
        MessageSequenceAsserter.messageSequence()
                .entering("methodWithAssignmentInReturnExpression", new Parameter<>("something", true))
                .valueChange("methodWithAssignmentInReturnExpression", "list", null)
                .valueChange("methodWithAssignmentInReturnExpression", "list", Collections.emptyList())
                .returning("methodWithAssignmentInReturnExpression", Collections.emptyList())
                .exiting("methodWithAssignmentInReturnExpression");
    }

    public List<?> methodWithAssignmentInReturnExpression(boolean something) {
        List<?> list = null;
        return something ? (list = Collections.emptyList()) : null; // having a mutation inside the ternary expression causes the issue.
    }

    @Test
    public void testShortCircuitedConditionalValueAssignment() {
        fetchedPreparedInMemoryTracer();
        enableValueChangeTracking();
        valueAssignmentInIfExpression();
        disableValueChangeTracking();
        MessageSequenceAsserter.messageSequence()
                .entering("valueAssignmentInIfExpression")
                .valueChange("valueAssignmentInIfExpression", "a", 1)
                .valueChange("valueAssignmentInIfExpression", "z", 3)
                // FIXME(snorbi07): we should have another value change event for 'b' once issue #55 is resolved
                .exiting("valueAssignmentInIfExpression");
    }

    public void valueAssignmentInIfExpression() {
        int a = 1;
        int z = 3;
        int b, c;
        if ((b = a) == 1 || (c = z) == 3) {

        }
        else {
            c = z;
            assert c == 3; // to avoid unused code issues
        }

        assert b == a; // to avoid unused code issues
    }

    @Test
    public void  testTryWithExpression() throws FileNotFoundException {
        fetchedPreparedInMemoryTracer();
        enableValueChangeTracking();
        try {
            tryWithExpression();
        }
        catch (Exception e) {
            assertTrue("Unexpected exception has occurred in test method", e != null);
        }
        disableValueChangeTracking();
        MessageSequenceAsserter.messageSequence()
                .entering("tryWithExpression")
                .entering("<init>", new Parameter<>("someValue", 22)) //AutoClosableClass constructor
                .valueChange("<init>", "this.someValue", 22)
                .exiting("<init>")
                .valueChange("tryWithExpression", "res", new AutoClosableClass(22))
                .entering("close") // class close method of AutoClosableClass constructor
                .exiting("close")
                .exiting("tryWithExpression");
    }

    private class AutoClosableClass<T> implements AutoCloseable {

        final private T someValue;

        public AutoClosableClass(T someValue) {
            this.someValue = someValue;
        }

        public T getSomeValue() {
            return someValue;
        }

        @Override
        public void close() throws Exception {
            // do nothing
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            AutoClosableClass<?> that = (AutoClosableClass<?>) o;

            return !(someValue != null ? !someValue.equals(that.someValue) : that.someValue != null);

        }

        @Override
        public int hashCode() {
            return someValue != null ? someValue.hashCode() : 0;
        }
    }

    public void tryWithExpression() throws Exception{
        try (AutoClosableClass res = new AutoClosableClass(22)) {
            // ...
            assert res != null;
        }
    }

    @Test
    public void  testTryWithMultipleExpression() throws FileNotFoundException {
        fetchedPreparedInMemoryTracer();
        enableValueChangeTracking();
        try {
            tryWithMultipleExpression();
        }
        catch (Exception e) {
            assertTrue("Unexpected exception has occurred in test method", e != null);
        }
        disableValueChangeTracking();
        MessageSequenceAsserter.messageSequence()
                .entering("tryWithMultipleExpression")
                .entering("<init>", new Parameter<>("someValue", 1))
                .valueChange("<init>", "this.someValue", 1)
                .exiting("<init>")
                .entering("<init>", new Parameter<>("someValue", 2))
                .valueChange("<init>", "this.someValue", 2)
                .exiting("<init>")
                .valueChange("tryWithMultipleExpression", "res1", new AutoClosableClass(1))
                .valueChange("tryWithMultipleExpression", "res2", new AutoClosableClass(2))
                .entering("close")
                .exiting("close")
                .entering("close")
                .exiting("close")
                .exiting("tryWithMultipleExpression");
    }

    public void tryWithMultipleExpression() throws Exception{
        try (AutoClosableClass res1 = new AutoClosableClass(1);
             AutoClosableClass res2 = new AutoClosableClass(2)) {
            // ...
            assert res1 != null;
            assert res2 != null;
        }
    }

    public int assignmentInReturnOfSwitchCase(int value) {
        int captureResult = 0;
        try {

            switch (value) {
                case 0:
                    return captureResult = 42;

                case 1:
                    return captureResult = 43;

                default:
                    return captureResult = 44;

            }
        }
        finally {
            if (captureResult == 0) {
                throw new IllegalStateException("this is just for avoiding unused variable issues...");
            }
        }
    }

    @Test
    public void testAssignmentInReturnOfSwitchCase() {
        fetchedPreparedInMemoryTracer();
        enableValueChangeTracking();
        assignmentInReturnOfSwitchCase(1);
        disableValueChangeTracking();
        MessageSequenceAsserter.messageSequence()
                .entering("assignmentInReturnOfSwitchCase", new Parameter<>("value", 1))
                .valueChange("assignmentInReturnOfSwitchCase", "captureResult", 0)
                .valueChange("assignmentInReturnOfSwitchCase", "captureResult", 43)
                .returning("assignmentInReturnOfSwitchCase", 43)
                .exiting("assignmentInReturnOfSwitchCase")
                .empty();
    }

    @Test
    public void testArrayIndexingWithUnaryOperator() {
        fetchedPreparedInMemoryTracer();
        enableValueChangeTracking();
        arrayIndexingWithUnaryOperator();
        disableValueChangeTracking();
        MessageSequenceAsserter.messageSequence()
                .entering("arrayIndexingWithUnaryOperator")
                .any() // new int[2] pointer changes at each execution...
                .valueChange("arrayIndexingWithUnaryOperator", "index", 0)
                .valueChange("arrayIndexingWithUnaryOperator", "index", 1)
                .valueChange("arrayIndexingWithUnaryOperator", "array[index - 1]", 2)
                .valueChange("arrayIndexingWithUnaryOperator", "index", 0)
                .valueChange("arrayIndexingWithUnaryOperator", "array[index]", 3)
                .valueChange("arrayIndexingWithUnaryOperator", "index", 1)
                .valueChange("arrayIndexingWithUnaryOperator", "array[index]", 4)
                .valueChange("arrayIndexingWithUnaryOperator", "index", 2)
                .valueChange("arrayIndexingWithUnaryOperator", "array[index - 1]", 5)
                .valueChange("arrayIndexingWithUnaryOperator", "index", 1)
                .valueChange("arrayIndexingWithUnaryOperator", "array[index]", 6)
                .valueChange("arrayIndexingWithUnaryOperator", "index", 0)
                .valueChange("arrayIndexingWithUnaryOperator", "array[index + 1]", 7)
                .exiting("arrayIndexingWithUnaryOperator")
                .empty();
    }

    public void arrayIndexingWithUnaryOperator() {
        int[] array = new int[2];
        int index = 0;
        array[index++] = 2;
        array[--index] = 3;
        array[++index] = 4;
        array[index++] = 5;
        array[--index] = 6;
        array[index--] = 7;
    }

    @Test
    public void testArrayIndexingWithAssignmentOperator() {
        fetchedPreparedInMemoryTracer();
        enableValueChangeTracking();
        arrayIndexingWithAssignmentOperator();
        disableValueChangeTracking();
        MessageSequenceAsserter.messageSequence()
                .entering("arrayIndexingWithAssignmentOperator")
                .any() // new int[2] pointer changes at each execution...
                .valueChange("arrayIndexingWithAssignmentOperator", "index", 0)
                .valueChange("arrayIndexingWithAssignmentOperator", "index", 1)
                .valueChange("arrayIndexingWithAssignmentOperator", "array[index]", 3)
                .valueChange("arrayIndexingWithAssignmentOperator", "index", 0)
                .valueChange("arrayIndexingWithAssignmentOperator", "array[index]", 4)
                .exiting("arrayIndexingWithAssignmentOperator")
                .empty();
    }

    public void arrayIndexingWithAssignmentOperator() {
        int[] array = new int[2];
        int index = 0;
        array[index += 1] = 3;
        array[index -= 1] = 4;
    }

}