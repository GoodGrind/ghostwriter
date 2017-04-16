package io.ghostwriter;


import io.ghostwriter.test.MessageSequenceAsserter;
import io.ghostwriter.test.Parameter;
import io.ghostwriter.test.TestBase;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;


@Ignore
public class LambdaTest extends TestBase {

    @Test
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
    public void testNestedLambdaSupport() {
        Function<Integer, Function<Integer, Integer>> adder = (base) -> (val) -> base + val;

        final InMemoryTracer inMemoryTracer = fetchedPreparedInMemoryTracer();
        inMemoryTracer.clearMessages();
        inMemoryTracer.enableValueChangeTracking();

        adder.apply(16).apply(1);

        MessageSequenceAsserter.messageSequence()
                .entering("testNestedLambdaSupport: (base)->", new Parameter<>("base", 16))
	    // FIXME(snorbi07): commented out until lambda support is fixed
            //    .any() // returning testNestedLambdaSupport: (base)-> ... comparing lambdas is not supported
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

    // NOTE(snorbi07): for now this is just a compilation check... if something is wrong it will fail to compile
    public static final class Item {
        private final double size;
        private final double value;

        private Item(final double size, final double value) {
            this.size = size;
            this.value = value;
        }

        public double getSize() {
            return size;
        }

        public double getValue() {
            return value;
        }

        private static Collector<Item, ?, Item> toSum() {
            return Collector.of(
                    () -> new double[2],
                    (a, b) -> {
                        a[0] += b.getSize();
                        a[1] += b.getValue();
                    },
                    (a, b) -> {
                        a[0] += b[0];
                        a[1] += b[1];
                        return a;
                    },
                    r -> new Item(r[0], r[1])
            );
        }
    }

    // NOTE(snorbi07): for now this is just a compilation check... if something is wrong it will fail to compile
    static public <K> void MYYYforEach(Consumer<? super K> action) {
        HashMap<K, Integer> map = new HashMap<>();
        map.forEach((k, v) -> action.accept(k));
    }

}
