package io.ghostwriter.openjdk.v7.common;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;

public interface Instrumenter {

    abstract class Option {
        public static final String GHOSTWRITER_EXCLUDE = "GHOSTWRITER_EXCLUDE";
        public static final String GHOSTWRITER_TRACE_VALUE_CHANGE = "GHOSTWRITER_TRACE_VALUE_CHANGE";
        public static final String GHOSTWRITER_TRACE_ON_ERROR = "GHOSTWRITER_TRACE_ON_ERROR";
        public static final String GHOSTWRITER_TRACE_RETURNING = "GHOSTWRITER_TRACE_RETURNING";
        public static final String GHOSTWRITER_ANNOTATED_ONLY = "GHOSTWRITER_ANNOTATED_ONLY";
        public static final String GHOSTWRITER_EXCLUDE_METHODS = "GHOSTWRITER_EXCLUDE_METHODS";
        public static final String GHOSTWRITER_INSTRUMENT = "GHOSTWRITER_INSTRUMENT";
        public static final String GHOSTWRITER_VERBOSE = "GHOSTWRITER_VERBOSE";
        public static final String GHOSTWRITER_SHORT_METHOD_LIMIT = "GHOSTWRITER_SHORT_METHOD_LIMIT";
    }

    void initialize(ProcessingEnvironment processingEnv);

    void process(Element element);

    boolean doInstrument();
}
