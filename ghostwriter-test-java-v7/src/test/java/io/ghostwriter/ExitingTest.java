package io.ghostwriter;

import io.ghostwriter.test.MessageSequenceAsserter;
import io.ghostwriter.test.Parameter;
import io.ghostwriter.test.TestBase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;


public class ExitingTest extends TestBase {

    @Test
    public void testMethodWithNoParametersAndNoResultExiting() {
        fetchedPreparedInMemoryTracer();
        methodWithNoParametersAndNoResult();

        MessageSequenceAsserter.messageSequence()
                .entering("methodWithNoParametersAndNoResult")
                .exiting("methodWithNoParametersAndNoResult");
    }

    private void methodWithNoParametersAndNoResult() {
        // instrumented code is tested
    }

    @Test
    public void testMethodWithIntegerReturnValueExiting() {
        fetchedPreparedInMemoryTracer();
        methodWithIntegerReturnValue();

        MessageSequenceAsserter.messageSequence()
                .entering("methodWithIntegerReturnValue")
                .returning("methodWithIntegerReturnValue", 42)
                .exiting("methodWithIntegerReturnValue");
    }

    private int methodWithIntegerReturnValue() {
        return 42;
    }

    @Test
    public void testMethodWithListReturnValueExiting() {
        fetchedPreparedInMemoryTracer();
        methodWithListReturnValue();

        List<Integer> expectedResult = new ArrayList<>();
        expectedResult.add(42);
        expectedResult.add(314);

        MessageSequenceAsserter.messageSequence()
                .entering("methodWithListReturnValue")
                .returning("methodWithListReturnValue", expectedResult)
                .exiting("methodWithListReturnValue");
    }

    private List<Integer> methodWithListReturnValue() {
        List<Integer> results = new ArrayList<>();
        results.add(42);
        results.add(314);
        return results;
    }

    // this is primarily a compilation test (issue #22 triggered this)
    @Test
    public void testMethodWithExplicitVoidReturnType() {
        fetchedPreparedInMemoryTracer();
        methodWithExplicitVoidReturnType();

        MessageSequenceAsserter.messageSequence()
                .entering("methodWithExplicitVoidReturnType")
                .returning("methodWithExplicitVoidReturnType", null)
                .exiting("methodWithExplicitVoidReturnType");
    }

    private Void methodWithExplicitVoidReturnType() {
        return null;
    }

    @Test
    public void testMethodWithGenericArrayResultType() {
        final Double[] expectedResultArray = new Double[]{1.1, 2.2, 3.3};
        fetchedPreparedInMemoryTracer();
        methodWithGenericArrayResultType(expectedResultArray);

        MessageSequenceAsserter.messageSequence()
                .entering("methodWithGenericArrayResultType", new Parameter<>("someArray", expectedResultArray))
                .returning("methodWithGenericArrayResultType", expectedResultArray)
                .exiting("methodWithGenericArrayResultType");
    }

    public <T> T[] methodWithGenericArrayResultType(T[] someArray) {
        return someArray;
    }

    @Test
    public void testMethodWithPrimitiveArrayResultType() {
        fetchedPreparedInMemoryTracer();
        final int[] expectedResultArray = new int[]{1, 2, 3};
        methodWithPrimitiveArrayResultType(expectedResultArray);

        MessageSequenceAsserter.messageSequence()
                .entering("methodWithPrimitiveArrayResultType", new Parameter<>("someArray", expectedResultArray))
                .returning("methodWithPrimitiveArrayResultType", expectedResultArray)
                .exiting("methodWithPrimitiveArrayResultType");
    }

    public int[] methodWithPrimitiveArrayResultType(int[] someArray) {
        return someArray;
    }

    @Test
    public void testMethodWithImplicitConversionAtReturn() {
        fetchedPreparedInMemoryTracer();
        methodWithImplicitConversionAtReturn(1L);

        MessageSequenceAsserter.messageSequence()
                .entering("methodWithImplicitConversionAtReturn", new Parameter<>("someValue", 1L))
                .returning("methodWithImplicitConversionAtReturn", 1L)
                .exiting("methodWithImplicitConversionAtReturn");
    }

    // Issue #46: this will fail to compile if we switch back to using boxed types for declaring the $result variable.
    // Long $result = 1 triggers and incompatible types compilation error. Expected 'int', got 'Long'.
    // Issue #75: added an extra type coercion step so the generic typed GW.returning class has the correct result.
    public static long methodWithImplicitConversionAtReturn(long someValue) {
        return  someValue > 0L ? 1 : -1;
    }

}
