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
public class GhostWriterAnnotationProcessor extends AbstractProcessor {

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

        Logger.initialize(processingEnv.getMessager());
        Logger.note(getClass(), "init", "beginning!");

        instrumenter.initialize(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return true;
        }

        Logger.note(getClass(), "process", "starting processing...");
        Set<? extends Element> elements = roundEnv.getRootElements();
        for (Element element : elements) {
            ElementKind kind = element.getKind();
            if (kind != null && kind.isClass()) {
                instrumenter.process(element);
            }
        }

        final boolean doClaimAnnotations = false;
        return doClaimAnnotations;
    }

}
