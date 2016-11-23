package io.ghostwriter.openjdk.v8.ast.translator;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import io.ghostwriter.openjdk.v7.ast.collector.ParameterCollector;
import io.ghostwriter.openjdk.v7.ast.compiler.JavaCompiler;
import io.ghostwriter.openjdk.v7.ast.compiler.JavaCompilerHelper;
import io.ghostwriter.openjdk.v7.ast.translator.EnteringExitingTranslator;
import io.ghostwriter.openjdk.v7.common.Logger;
import io.ghostwriter.openjdk.v7.model.Method;
import io.ghostwriter.openjdk.v7.model.Parameter;

import java.util.stream.Collectors;


public class LambdaAwareEnteringExitingTranslator extends EnteringExitingTranslator {

    public LambdaAwareEnteringExitingTranslator(JavaCompiler javac, JavaCompilerHelper helper) {
        super(javac, helper);
    }

    private JCTree.JCLambda visitedLambda = null;

    @Override
    public void visitLambda(JCTree.JCLambda jcLambda) {
        final boolean isLambdaBodyWrappedInBlock = jcLambda.body instanceof JCTree.JCBlock;
        if (!isLambdaBodyWrappedInBlock) {
            throw new IllegalStateException("Lambda does not have a body, got: " + jcLambda.body.getClass() + "," + jcLambda.toString());
            // we run the LambdaAwareWrapInBlockTranslator to ensure that all lambdas have a body block.
            // it that is not the case there is an error somewhere else!
        }
        visitedLambda = jcLambda;

        JCTree.JCBlock bodyBlock = (JCTree.JCBlock) jcLambda.body;
        final List<JCTree.JCStatement> originalBody = bodyBlock.getStatements();
        final Method enclosingMethod = getMethod();
        final List<JCTree.JCStatement> instrumentedBody = instrumentedBody(enclosingMethod, originalBody);
        bodyBlock.stats = instrumentedBody;

        visitedLambda = null;
        super.visitLambda(jcLambda);
    }

    @Override
    protected JCTree.JCExpressionStatement enteringExpression(Method model) {
        if (visitedLambda == null) {
            return super.enteringExpression(model);
        }
        return enteringLambdaExpression(model, visitedLambda);
    }

    @Override
    protected JCTree.JCExpressionStatement exitingExpression(Method model) {
        if (visitedLambda == null) {
            return super.exitingExpression(model);
        }
        return exitingLambdaExpression(model, visitedLambda);
    }

    private JCTree.JCExpressionStatement exitingLambdaExpression(Method model, JCTree.JCLambda visitedLambda) {
        String exitingHandler = getExitingHandler();
        if (exitingHandler == null || "".equals(exitingHandler)) {
            Logger.error(getClass(), "exitingLambdaExpression",
                    "invalid fully qualified name for 'exiting' handler: " + String.valueOf(exitingHandler));
        }

        JCTree.JCExpression exitingHandlerExpression = getJavac().expression(exitingHandler);
        ListBuffer<JCTree.JCExpression> handlerArguments = new ListBuffer<>();

        JCTree.JCExpression thisOrClass = getJavacHelper().methodContext(model);
        handlerArguments.add(thisOrClass);

        final String lambdaName = Lambdas.nameFor(model, visitedLambda);
        JCTree.JCLiteral methodName = getJavac().literal(lambdaName);
        handlerArguments.add(methodName);

        return getJavac().call(exitingHandlerExpression, handlerArguments.toList());
    }

    protected JCTree.JCExpressionStatement enteringLambdaExpression(Method model, JCTree.JCLambda visitedLambda) {
        String enteringHandler = getEnteringHandler();
        if (enteringHandler == null || "".equals(enteringHandler)) {
            Logger.error(getClass(), "enteringLambdaExpression",
                    "invalid fully qualified name for 'entering' handler: " + String.valueOf(enteringHandler));
        }

        JCTree.JCExpression enteringHandlerExpression = getJavac().expression(enteringHandler);
        ListBuffer<JCTree.JCExpression> handlerArguments = new ListBuffer<>();

        JCTree.JCExpression thisOrClass = getJavacHelper().methodContext(model);
        handlerArguments.add(thisOrClass);

        final String lambdaName = Lambdas.nameFor(model, visitedLambda);
        JCTree.JCLiteral methodName = getJavac().literal(lambdaName);
        handlerArguments.add(methodName);

        final java.util.List<Parameter> lambdaParameters
                = new ParameterCollector(getJavac(), visitedLambda).toList();

        JCTree.JCExpression methodArguments = enteringHandlerParameterArray(lambdaParameters);
        handlerArguments.add(methodArguments);

        return getJavac().call(enteringHandlerExpression, handlerArguments.toList());
    }

}
