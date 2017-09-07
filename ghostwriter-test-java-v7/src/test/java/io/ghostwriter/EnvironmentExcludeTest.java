package io.ghostwriter;

import io.ghostwriter.excluded.ExcludedPackageClass;
import io.ghostwriter.excluded.nested.ExcludedNestedPackageClass;
import io.ghostwriter.test.TestBase;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class EnvironmentExcludeTest extends TestBase {

    @Test
    public void testExcludedPackageClass() {
        final InMemoryTracer inMemoryTracer = fetchedPreparedInMemoryTracer();
        inMemoryTracer.clearMessages();
        ExcludedPackageClass someVal = new ExcludedPackageClass();
        someVal.meaningOfLife();
        final int numMessages = inMemoryTracer.numberOfMessages();
        assertEquals("Class in excluded package should not produce GW events!",
              Collections.emptyList(), new ArrayList<>(inMemoryTracer.getMessages()));
    }

    @Test
    public void testExcludedNestedPackageClass() {
        final InMemoryTracer inMemoryTracer = fetchedPreparedInMemoryTracer();
        inMemoryTracer.clearMessages();
        ExcludedNestedPackageClass someVal = new ExcludedNestedPackageClass();
        someVal.meaningOfLife();
        final int numMessages = inMemoryTracer.numberOfMessages();
        assertEquals("Class in excluded package should not produce GW events!",
                Collections.emptyList(), new ArrayList<>(inMemoryTracer.getMessages()));
    }

    @Test
    public void testExcludedClass() {
        final InMemoryTracer inMemoryTracer = fetchedPreparedInMemoryTracer();
        inMemoryTracer.clearMessages();
        ExcludedByEnvironmentClass someVal = new ExcludedByEnvironmentClass();
        someVal.meaningOfLife();
        final int numMessages = inMemoryTracer.numberOfMessages();
        assertEquals("Class excluded by environment should not produce GW events!",
                Collections.emptyList(), new ArrayList<>(inMemoryTracer.getMessages()));
    }
}
