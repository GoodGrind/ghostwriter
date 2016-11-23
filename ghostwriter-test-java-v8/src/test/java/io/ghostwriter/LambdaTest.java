package io.ghostwriter;


import io.ghostwriter.test.MessageSequenceAsserter;
import io.ghostwriter.test.Parameter;
import io.ghostwriter.test.TestBase;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;


public class LambdaTest extends TestBase {

    @Ignore
    public void testEntering() {
        fetchedPreparedInMemoryTracer();
        Function<Integer, String> lambda = (val) -> String.valueOf(val);

        lambda.apply(123);

        MessageSequenceAsserter.messageSequence()
                .entering("testEntering: (val)->", new Parameter<>("val", 123))
                // FIXME(snorbi07): enable after Lambda support fix
                //.returning("testEntering: (val)->", "123")
                .exiting("testEntering: (val)->");
    }

    @Test
    @Ignore
    public void testBlockBasedLambdaSupport() {
        Function<Integer, String> lambda = (val) -> {
            Integer newVal = val + 1;
            String str = String.valueOf(newVal);
            return str;
        };

        final InMemoryTracer inMemoryTracer = fetchedPreparedInMemoryTracer();
        inMemoryTracer.clearMessages();
        inMemoryTracer.enableValueChangeTracking();

        lambda.apply(123);

        MessageSequenceAsserter.messageSequence()
                .entering("testBlockBasedLambdaSupport: (val)->", new Parameter<>("val", 123))
                .valueChange("testBlockBasedLambdaSupport: (val)->", "newVal", 124)
                .valueChange("testBlockBasedLambdaSupport: (val)->", "str", "124")
                // FIXME(snorbi07): enable after Lambda support fix
                //.returning("testBlockBasedLambdaSupport: (val)->", "124")
                .exiting("testBlockBasedLambdaSupport: (val)->")
                .empty();
    }

    @Test
    @Ignore
    public void testNestedLambdaSupport() {
        Function<Integer, Function<Integer, Integer>> adder = (base) -> (val) -> base + val;

        final InMemoryTracer inMemoryTracer = fetchedPreparedInMemoryTracer();
        inMemoryTracer.clearMessages();
        inMemoryTracer.enableValueChangeTracking();

        adder.apply(16).apply(1);

        MessageSequenceAsserter.messageSequence()
                .entering("testNestedLambdaSupport: (base)->", new Parameter<>("base", 16))
                .any() // returning testNestedLambdaSupport: (base)-> ... comparing lambdas is not supported
                .exiting("testNestedLambdaSupport: (base)->")
                .entering("testNestedLambdaSupport: (val)->", new Parameter<>("val", 1))
                // FIXME(snorbi07): enable after Lambda support fix
                //.returning("testNestedLambdaSupport: (val)->", 17)
                .exiting("testNestedLambdaSupport: (val)->");
    }

    public <T> List<List<T>> testLambdaTypeInference() {
        java.util.function.Supplier<List<List<T>>> someCollectionSupplier;
        someCollectionSupplier = Collections::emptyList;
        return someCollectionSupplier.get();
    }

    // TODO: test this scoping
    // TODO: test class scoping
    // TODO: test lambda properties (not in a method context, is the generated name correct?)

}
