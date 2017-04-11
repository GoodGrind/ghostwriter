package io.ghostwriter.openjdk.v8.ast.translator;

import com.sun.tools.javac.tree.JCTree;
import io.ghostwriter.openjdk.v7.ast.compiler.JavaCompiler;
import io.ghostwriter.openjdk.v7.ast.translator.WrapInBlockTranslator;

import static com.sun.source.tree.LambdaExpressionTree.BodyKind;


public class LambdaAwareWrapInBlockTranslator extends WrapInBlockTranslator {

    public LambdaAwareWrapInBlockTranslator(JavaCompiler javac) {
        super(javac);
    }

    @Override
    public void visitLambda(JCTree.JCLambda jcLambda) {
        final boolean isLambdaExpression = BodyKind.EXPRESSION.equals(jcLambda.getBodyKind());
        if (isLambdaExpression && Lambdas.doInstrumentLambdas()) {
            // in case the lambda body is a single expression, we need to wrap it in a return statement
            final JCTree body = jcLambda.body;
            final JCTree.JCExpression bodyExpression = (JCTree.JCExpression) body;
            final JCTree.JCReturn returnLambdaExpressionResult = javac.makeReturn(bodyExpression);
            jcLambda.body = wrappedBody(returnLambdaExpressionResult);
        }

        super.visitLambda(jcLambda);
    }
}
