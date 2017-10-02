package io.ghostwriter.openjdk.v8;

import com.sun.source.util.Trees;
import io.ghostwriter.openjdk.v7.Javac7Instrumenter;
import io.ghostwriter.openjdk.v7.ast.compiler.JavaCompilerHelper;
import io.ghostwriter.openjdk.v7.ast.translator.Translator;
import io.ghostwriter.openjdk.v7.model.Method;
import io.ghostwriter.openjdk.v8.ast.compiler.Javac;
import io.ghostwriter.openjdk.v8.ast.translator.LambdaAwareMethodTranslator;

import javax.annotation.processing.ProcessingEnvironment;


public class Javac8Instrumenter extends Javac7Instrumenter {

    @Override
    public void initialize(ProcessingEnvironment processingEnv) {
        setTrees(Trees.instance(processingEnv));
        final Javac javac8 = new Javac(processingEnv);
        setJavac(javac8);
        setJavacHelper(new JavaCompilerHelper(javac8));
        initializeFromEnv(processingEnv);
    }

    @Override
    protected Translator<Method> getMethodTranslator() {
        return new LambdaAwareMethodTranslator(getJavac(), getJavacHelper());
    }
}
