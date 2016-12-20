package io.ghostwriter.openjdk.v8.ast.translator;

import io.ghostwriter.openjdk.v7.ast.compiler.JavaCompiler;
import io.ghostwriter.openjdk.v7.ast.compiler.JavaCompilerHelper;
import io.ghostwriter.openjdk.v7.ast.translator.MethodTranslator;
import io.ghostwriter.openjdk.v7.ast.translator.ReturnExpressionMutationExtractionTranslator;
import io.ghostwriter.openjdk.v7.model.Method;


public class LambdaAwareMethodTranslator extends MethodTranslator {

    public LambdaAwareMethodTranslator(JavaCompiler javac, JavaCompilerHelper helper) {
        super(javac, helper);
    }

    @Override
    protected void transformToBlockConstructs(Method model) {
        final LambdaAwareWrapInBlockTranslator wrapInBlockTranslator =
                new LambdaAwareWrapInBlockTranslator(getJavac());
        wrapInBlockTranslator.translate(model);
    }

    @Override
    protected void traceEnteringExiting(Method model) {
        LambdaAwareEnteringExitingTranslator enteringExitingTranslator =
                new LambdaAwareEnteringExitingTranslator(getJavac(), getHelper());
        enteringExitingTranslator.translate(model);
    }

    @Override
    protected void traceReturn(Method model) {
        LambdaAwareReturningTranslator returningTranslator =
                new LambdaAwareReturningTranslator(getJavac(), getHelper());
        returningTranslator.translate(model);
    }

    @Override
    protected void traceValueChanges(Method model) {
        ReturnExpressionMutationExtractionTranslator returnExpressionTranslator =
                new ReturnExpressionMutationExtractionTranslator(getJavac(), getHelper());
        returnExpressionTranslator.translate(model);

        LambdaAwareValueChangeTranslator valueChangeTranslator =
                new LambdaAwareValueChangeTranslator(getJavac(), getHelper());
        valueChangeTranslator.translate(model);
    }

}
