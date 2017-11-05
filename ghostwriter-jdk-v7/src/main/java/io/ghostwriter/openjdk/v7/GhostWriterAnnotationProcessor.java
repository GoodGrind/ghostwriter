package io.ghostwriter.openjdk.v7;


import io.ghostwriter.openjdk.v7.common.Instrumenter;
import io.ghostwriter.openjdk.v7.common.Logger;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.util.Set;

@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedOptions({Instrumenter.Option.GHOSTWRITER_ANNOTATED_ONLY,
        Instrumenter.Option.GHOSTWRITER_EXCLUDE,
        Instrumenter.Option.GHOSTWRITER_TRACE_ON_ERROR,
        Instrumenter.Option.GHOSTWRITER_TRACE_RETURNING,
        Instrumenter.Option.GHOSTWRITER_TRACE_VALUE_CHANGE,
        Instrumenter.Option.GHOSTWRITER_EXCLUDE_METHODS,
        Instrumenter.Option.GHOSTWRITER_INSTRUMENT,
        Instrumenter.Option.GHOSTWRITER_VERBOSE,
        Instrumenter.Option.GHOSTWRITER_SHORT_METHOD_LIMIT})
public class GhostWriterAnnotationProcessor extends AbstractProcessor {

    // part of the Annotation processor API. Since GhostWriter just hijacks the processor pipeline
    // we take care not to claim any annotations and thus always return false.
    protected static boolean NO_ANNOTATIONS_CLAIMED = false;

    private Instrumenter instrumenter;

    public GhostWriterAnnotationProcessor(Instrumenter instrumenter) {
        this.instrumenter = instrumenter;
    }

    public GhostWriterAnnotationProcessor() {
        this(new Javac7Instrumenter());
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        instrumenter.initialize(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return NO_ANNOTATIONS_CLAIMED;
        }

        if (!instrumenter.doInstrument()) {
            Logger.note(getClass(), "process", "skipping processing...");
            return NO_ANNOTATIONS_CLAIMED;
        }

        Logger.note(getClass(), "process", "starting processing...");
        Set<? extends Element> elements = roundEnv.getRootElements();
        for (Element element : elements) {
            ElementKind kind = element.getKind();
            if (kind != null && kind.isClass()) {
                instrumenter.process(element);
            }
        }

        return NO_ANNOTATIONS_CLAIMED;
    }

}
