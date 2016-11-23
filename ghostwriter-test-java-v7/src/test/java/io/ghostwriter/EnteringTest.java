package io.ghostwriter;

import io.ghostwriter.message.EnteringMessage;
import io.ghostwriter.message.Message;
import io.ghostwriter.test.TestBase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class EnteringTest extends TestBase {

    @Test
    public void testMethodWithNoParametersAndNoResultEntering() {
        InMemoryTracer inMemoryTracer = fetchedPreparedInMemoryTracer();
        methodWithNoParametersAndNoResult();
        // pop the unused exitingMessage
        inMemoryTracer.popMessage();
        Message<?> enteringMessage = inMemoryTracer.popMessage();
        boolean isEnteringMessage = enteringMessage instanceof EnteringMessage;
        boolean isValidEnteringMessage = false;
        if (isEnteringMessage) {
            EnteringMessage msg = (EnteringMessage) enteringMessage;
            String methodName = msg.getMethod();
            boolean hasNoParameters = !msg.hasParameters();
            boolean hasCorrectName = "methodWithNoParametersAndNoResult".equals(methodName);
            isValidEnteringMessage = hasCorrectName && hasNoParameters;
        }
        assertTrue("Not a valid entering message!", isEnteringMessage && isValidEnteringMessage);
    }

    private void methodWithNoParametersAndNoResult() {
        // instrument code tested here
    }

    @Test
    public void testMethodWithSinglePrimitiveParameterAndNoResultEntering() {
        InMemoryTracer inMemoryTracer = fetchedPreparedInMemoryTracer();
        final int VALUE_PASSED_TO_FUNCTION = 42;
        methodWithSinglePrimitiveParameterAndNoResult(VALUE_PASSED_TO_FUNCTION);
        // pop the unused exitingMessage
        inMemoryTracer.popMessage();

        Message<?> enteringMessage = inMemoryTracer.popMessage();
        boolean isEnteringMessage = enteringMessage instanceof EnteringMessage;
        assertTrue("Invalid message type: " + enteringMessage.getClass(), isEnteringMessage);

        EnteringMessage msg = (EnteringMessage) enteringMessage;
        boolean hasParameters = msg.hasParameters();
        assertTrue("Parameter information not traced", hasParameters);

        EnteringMessage.BasePayload payload = msg.getPayload();
        Object[] parameters = payload.getParameters();
        boolean hasCorrectNumberOfParameters = parameters.length == NUMBER_OF_ENTRIES_PER_TRACED_PARAMETER; // we should only have a single parameter
        assertTrue("Missing or excess parameter tracking. Number of parameters: " + parameters.length, hasCorrectNumberOfParameters);

        final int EXPECTED_INDEX_OF_FIRST_PARAMETER_NAME = 0;
        Object unusedPrimitiveParameterName = parameters[EXPECTED_INDEX_OF_FIRST_PARAMETER_NAME];
        boolean doesFirstEntryContainTheName = unusedPrimitiveParameterName instanceof String;
        assertTrue("Incorrect entry order. Parameter name has type of: " + unusedPrimitiveParameterName.getClass(), doesFirstEntryContainTheName);

        boolean isCorrectNameExtracted = "unusedPrimitive".equals(unusedPrimitiveParameterName);
        assertTrue("Parameter name does not match the expected! Got: " + unusedPrimitiveParameterName, isCorrectNameExtracted);

        final int EXPECTED_INDEX_OF_FIRST_PARAMETER_VALUE = 1;
        Object unusedPrimitiveParameterValue = parameters[EXPECTED_INDEX_OF_FIRST_PARAMETER_VALUE];
        boolean doesTracedValueHaveCorrectType = unusedPrimitiveParameterValue instanceof Integer;
        assertTrue("Parameter value does not have the expected type! Got: " + unusedPrimitiveParameterValue.getClass(), doesTracedValueHaveCorrectType);

        int tracedValue = (int) unusedPrimitiveParameterValue;
        boolean doesHaveCorrectValueTraced = tracedValue == VALUE_PASSED_TO_FUNCTION;
        assertTrue("Parameter value does not match the expected value! Got: " + tracedValue, doesHaveCorrectValueTraced);
    }

    private void methodWithSinglePrimitiveParameterAndNoResult(int unusedPrimitive) {
        assert (unusedPrimitive == unusedPrimitive); // just so we don't get a warning/note/advice/rant that this is unused
    }

    @Test
    public void testMethodWithTwoParametersAndNoResultEntering() {
        InMemoryTracer inMemoryTracer = fetchedPreparedInMemoryTracer();
        // prepare passed parameters
        final String fistPassedValue = "myUnusedString";
        final List<Integer> secondPassedValue = new ArrayList<>();
        secondPassedValue.add(3);
        secondPassedValue.add(5);
        secondPassedValue.add(7);

        // call traced test method
        methodWithTwoParametersAndNoResult(fistPassedValue, secondPassedValue);

        // pop the unused exitingMessage
        inMemoryTracer.popMessage();

        // get entering message
        Message<?> enteringMessage = inMemoryTracer.popMessage();
        boolean isEnteringMessage = enteringMessage instanceof EnteringMessage;
        assertTrue("Invalid message type: " + enteringMessage.getClass(), isEnteringMessage);

        EnteringMessage msg = (EnteringMessage) enteringMessage;
        boolean hasParameters = msg.hasParameters();
        assertTrue("Parameter information not traced", hasParameters);

        // verify that we have the expected number of entries
        EnteringMessage.BasePayload payload = msg.getPayload();
        Object[] parameters = payload.getParameters();
        final int NUMBER_OF_PARAMETERS = 2;
        final int NUMBER_OF_EXPECTED_PARAMETER_ENTRIES = NUMBER_OF_ENTRIES_PER_TRACED_PARAMETER * NUMBER_OF_PARAMETERS;
        boolean hasCorrectNumberOfParameters = parameters.length == NUMBER_OF_EXPECTED_PARAMETER_ENTRIES;
        assertTrue("Missing or excess parameter tracking. Number of parameters: " + parameters.length, hasCorrectNumberOfParameters);

        // verify the name type of the first parameter
        final int EXPECTED_INDEX_OF_FIRST_PARAMETER_NAME = 0;
        Object unusedStringParameterName = parameters[EXPECTED_INDEX_OF_FIRST_PARAMETER_NAME];
        boolean doesFirstEntryContainTheName = unusedStringParameterName instanceof String;
        assertTrue("Incorrect entry order. Parameter name has type of: " + unusedStringParameterName.getClass(), doesFirstEntryContainTheName);

        // verify the name of the first parameter
        boolean isCorrectNameExtractedForUnusedString = "unusedString".equals(unusedStringParameterName);
        assertTrue("Parameter name does not match the expected! Got: " + unusedStringParameterName, isCorrectNameExtractedForUnusedString);

        // verify the value type of the first traced parameter
        final int EXPECTED_INDEX_OF_FIRST_PARAMETER_VALUE = 1;
        Object unusedStringValue = parameters[EXPECTED_INDEX_OF_FIRST_PARAMETER_VALUE];
        assertTrue("First parameter value does not have the expected type! Got: " + unusedStringValue.getClass(), unusedStringValue instanceof String);

        // verify the actual traced value of the first parameter
        String tracedStringValue = (String) unusedStringValue;
        boolean doesHaveCorrectValueTraced = tracedStringValue.equals(fistPassedValue);
        assertTrue("First parameter value does not match the expected value! Got: " + tracedStringValue, doesHaveCorrectValueTraced);

        // verify the name type for the second traced parameter
        final int EXPECTED_INDEX_OF_SECOND_PARAMETER_NAME = 2;
        Object unusedListParameterName = parameters[EXPECTED_INDEX_OF_SECOND_PARAMETER_NAME];
        boolean doesThirdEntryContainTheName = unusedListParameterName instanceof String;
        assertTrue("Incorrect entry order. Second parameter name has type of: " + unusedStringParameterName.getClass(), doesThirdEntryContainTheName);

        // verify the name of the second traced parameter
        boolean isCorrectNameExtractedForUnusedList = "unusedList".equals(unusedListParameterName);
        assertTrue("Parameter name does not match the expected! Got: " + unusedListParameterName, isCorrectNameExtractedForUnusedList);

        // verify the value type of the second traced parameter
        final int EXPECTED_INDEX_OF_SECOND_PARAMETER_VALUE = 3;
        Object unusedListValue = parameters[EXPECTED_INDEX_OF_SECOND_PARAMETER_VALUE];
        assertTrue("Second parameter value does not have the expected type! Got: " + unusedListValue.getClass(), unusedListValue instanceof List<?>);

        // verify the value of the second traced parameter
        List<Integer> tracedListValue = (List<Integer>) unusedListValue;
        assertTrue("Second parameter value does not match the expected value! Got: " + tracedListValue, secondPassedValue.equals(tracedListValue));
    }

    private void methodWithTwoParametersAndNoResult(String unusedString, List<Integer> unusedList) {
        assert (unusedString != null);
        assert (unusedList != null);
    }

    @Test
    public void testArraySupport() {
        InMemoryTracer inMemoryTracer = fetchedPreparedInMemoryTracer();
        final int[] someArray = {0, 1, 2};  // just to verify that array indexing doesn't cause problems
        final String[] inputArray = {"Hello", "World"};
        methodWithArrayParameter(someArray[0], inputArray);

        // pop the unused exitingMessage
        inMemoryTracer.popMessage();

        // pop the unused returningMessage
        inMemoryTracer.popMessage();

        // get entering message
        Message<?> enteringMessage = inMemoryTracer.popMessage();
        boolean isEnteringMessage = enteringMessage instanceof EnteringMessage;
        assertTrue("Invalid message type: " + enteringMessage.getClass(), isEnteringMessage);

        EnteringMessage msg = (EnteringMessage) enteringMessage;
        boolean hasParameters = msg.hasParameters();
        assertTrue("Parameter information not traced", hasParameters);

        // verify that we have the expected number of entries
        EnteringMessage.BasePayload payload = msg.getPayload();
        Object[] parameters = payload.getParameters();
        final int NUMBER_OF_PARAMETERS = 2;
        final int NUMBER_OF_EXPECTED_PARAMETER_ENTRIES = NUMBER_OF_ENTRIES_PER_TRACED_PARAMETER * NUMBER_OF_PARAMETERS;
        boolean hasCorrectNumberOfParameters = parameters.length == NUMBER_OF_EXPECTED_PARAMETER_ENTRIES;
        assertTrue("Missing or excess parameter tracking. Number of parameters: " + parameters.length, hasCorrectNumberOfParameters);

        // verify the name type of the first parameter
        final int EXPECTED_INDEX_OF_FIRST_PARAMETER_NAME = 0;
        Object indexParameterName = parameters[EXPECTED_INDEX_OF_FIRST_PARAMETER_NAME];
        boolean doesFirstEntryContainTheName = indexParameterName instanceof String;
        assertTrue("Incorrect entry order. Parameter name has type of: " + indexParameterName.getClass(), doesFirstEntryContainTheName);

        // verify the name of the first parameter
        boolean isCorrectNameExtractedForUnusedString = "index".equals(indexParameterName);
        assertTrue("Parameter name does not match the expected! Got: " + indexParameterName, isCorrectNameExtractedForUnusedString);

        // verify the value type of the first traced parameter
        final int EXPECTED_INDEX_OF_FIRST_PARAMETER_VALUE = 1;
        Object unusedStringValue = parameters[EXPECTED_INDEX_OF_FIRST_PARAMETER_VALUE];
        assertTrue("First parameter value does not have the expected type! Got: " + unusedStringValue.getClass(), unusedStringValue instanceof Integer);

        // verify the actual traced value of the first parameter
        Integer tracedIndexValue = (Integer) unusedStringValue;
        boolean doesHaveCorrectValueTraced = tracedIndexValue.equals(someArray[0]);
        assertTrue("First parameter value does not match the expected value! Got: " + tracedIndexValue, doesHaveCorrectValueTraced);

        // verify the name type for the second traced parameter
        final int EXPECTED_INDEX_OF_SECOND_PARAMETER_NAME = 2;
        Object unusedListParameterName = parameters[EXPECTED_INDEX_OF_SECOND_PARAMETER_NAME];
        boolean doesThirdEntryContainTheName = unusedListParameterName instanceof String;
        assertTrue("Incorrect entry order. Second parameter name has type of: " + indexParameterName.getClass(), doesThirdEntryContainTheName);

        // verify the name of the second traced parameter
        boolean isCorrectNameExtractedForUnusedList = "args".equals(unusedListParameterName);
        assertTrue("Parameter name does not match the expected! Got: " + unusedListParameterName, isCorrectNameExtractedForUnusedList);

        // verify the value type of the second traced parameter
        final int EXPECTED_INDEX_OF_SECOND_PARAMETER_VALUE = 3;
        Object argsValue = parameters[EXPECTED_INDEX_OF_SECOND_PARAMETER_VALUE];
        assertTrue("Second parameter value does not have the expected type! Got: " + argsValue.getClass(), argsValue instanceof Object[]);

        // verify the value of the second traced parameter
        String[] tracedArrayValue = (String[]) argsValue;
        assertTrue("Second parameter value does not match the expected value! Got: " + tracedArrayValue, Arrays.deepEquals(tracedArrayValue, inputArray));
    }

    private String methodWithArrayParameter(int index, String[] args) {
        return args[index];
    }

}
