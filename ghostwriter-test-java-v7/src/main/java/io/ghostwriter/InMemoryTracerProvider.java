package io.ghostwriter;

public enum InMemoryTracerProvider implements TracerProvider<InMemoryTracer> {
    INSTANCE;

    private final InMemoryTracer tracer;

    InMemoryTracerProvider() {
        tracer = new InMemoryTracer();
    }

    @Override
    public InMemoryTracer getTracer() {
        return tracer;
    }

}
