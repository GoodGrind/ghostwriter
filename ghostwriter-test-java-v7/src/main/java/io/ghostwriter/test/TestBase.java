package io.ghostwriter.test;

import io.ghostwriter.GhostWriter;
import io.ghostwriter.InMemoryTracer;
import io.ghostwriter.InMemoryTracerProvider;
import org.junit.Before;
import org.junit.BeforeClass;


/**
 * Main reason for this class to be under main and not test is to exclude it from GW instrumentation
 */
public class TestBase {

    public static final int NUMBER_OF_ENTRIES_PER_TRACED_PARAMETER = 2; // first value should be the name, the second one is the value of the parameter

    @BeforeClass
    public static void setup() {
        // switch to the in-memory implementation, so we can do unit testing.
        GhostWriter.setTracerProvider(InMemoryTracerProvider.INSTANCE);
    }

    @Before
    public void configureInMemoryTracer() {
        fetchedPreparedInMemoryTracer();
    }

    //FIXME(snorbi07): refactor to a more meaningful name... the difficulty is coming up with one
    public InMemoryTracer fetchedPreparedInMemoryTracer() {
        InMemoryTracer tracer = InMemoryTracerProvider.INSTANCE.getTracer();
        // we only need value change tracker in the specific tests and there we enable it manually
        tracer.disableValueChangeTracking();
        // make sure that entering/exiting tracing is enabled.
        tracer.enableEnteringExitingTracking();
        // we clear manually because the @Test method is also traced and it pollutes the entering logs.
        tracer.clearMessages();

        return tracer;
    }

    public void disableValueChangeTracking() {
        InMemoryTracerProvider.INSTANCE.getTracer().disableValueChangeTracking();
    }

    public void enableValueChangeTracking() {
        InMemoryTracerProvider.INSTANCE.getTracer().enableValueChangeTracking();
    }

}
