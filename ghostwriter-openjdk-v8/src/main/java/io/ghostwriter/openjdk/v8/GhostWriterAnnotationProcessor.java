package io.ghostwriter.openjdk.v8;

import io.ghostwriter.annotation.Exclude;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class GhostWriterAnnotationProcessor extends io.ghostwriter.openjdk.v7.GhostWriterAnnotationProcessor {

    @Exclude
    public GhostWriterAnnotationProcessor() {
        super(new Javac8Instrumenter());
    }

}
