package io.ghostwriter.openjdk.v7.common;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;

public interface Instrumenter {

    void initialize(ProcessingEnvironment processingEnv);

    void process(Element element);

}
