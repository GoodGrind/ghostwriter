package io.ghostwriter.openjdk.v8;

import io.ghostwriter.annotation.Exclude;
import io.ghostwriter.openjdk.v7.common.Instrumenter;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions({Instrumenter.Option.GHOSTWRITER_ANNOTATED_ONLY,
        Instrumenter.Option.GHOSTWRITER_EXCLUDE,
        Instrumenter.Option.GHOSTWRITER_TRACE_ON_ERROR,
        Instrumenter.Option.GHOSTWRITER_TRACE_RETURNING,
        Instrumenter.Option.GHOSTWRITER_TRACE_VALUE_CHANGE,
        Instrumenter.Option.GHOSTWRITER_EXCLUDE_METHODS,
        Instrumenter.Option.GHOSTWRITER_INSTRUMENT,
        Instrumenter.Option.GHOSTWRITER_VERBOSE})
public class GhostWriterAnnotationProcessor extends io.ghostwriter.openjdk.v7.GhostWriterAnnotationProcessor {

    @Exclude
    public GhostWriterAnnotationProcessor() {
        super(new Javac8Instrumenter());
    }

}
