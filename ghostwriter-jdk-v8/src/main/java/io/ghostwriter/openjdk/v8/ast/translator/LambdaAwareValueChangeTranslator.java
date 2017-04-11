package io.ghostwriter.openjdk.v8.ast.translator;


import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.ListBuffer;
import io.ghostwriter.openjdk.v7.ast.compiler.JavaCompiler;
import io.ghostwriter.openjdk.v7.ast.compiler.JavaCompilerHelper;
import io.ghostwriter.openjdk.v7.ast.translator.ValueChangeTranslator;
import io.ghostwriter.openjdk.v7.common.Logger;
import io.ghostwriter.openjdk.v7.model.Method;

public class LambdaAwareValueChangeTranslator extends ValueChangeTranslator {

    private JCTree.JCLambda visitedLambda;

    public LambdaAwareValueChangeTranslator(JavaCompiler javac, JavaCompilerHelper helper) {
        super(javac, helper);
    }

    @Override
    public void visitLambda(JCTree.JCLambda jcLambda) {
        // we capture the traversed lambda regardless whether lambda instrumentation is turned on or not
        // since this is the only way we know that we are inside a JCBlock in a lambda expression

        visitedLambda = jcLambda;
        super.visitLambda(jcLambda);
        visitedLambda = null;
    }

    @Override
    public void visitBlock(JCTree.JCBlock block) {
        // we always capture value changes in a block if we are not traversing a lambda (visitedLambda == null)
        // otherwise this is the place where we have to decide to instrument lambdas or not
        if (visitedLambda == null || Lambdas.doInstrumentLambdas()) {
            super.visitBlock(block);
            return;
        }
        result = block;
    }

    @Override
    protected JCTree.JCExpressionStatement valueChangeExpression(Method model, JCTree.JCExpression variable) {
        if (visitedLambda == null) {
            return super.valueChangeExpression(model, variable);
        }

        String valueChangeHandler = getValueChangeHandler();
        if (valueChangeHandler == null || "".equals(valueChangeHandler)) {
            Logger.error(getClass(), "valueChangeExpression", "invalid fully qualified name for 'valueChange' handler: " + String.valueOf(valueChangeHandler));
        }

        JCTree.JCExpression valueChangeHandlerExpression = getJavac().expression(valueChangeHandler);
        ListBuffer<JCTree.JCExpression> handlerArguments = new ListBuffer<>();

        JCTree.JCExpression thisOrClass = getHelper().methodContext(model);
        handlerArguments.add(thisOrClass);

        final String lambdaName = Lambdas.nameFor(model, visitedLambda);
        JCTree.JCLiteral methodName = getJavac().literal(lambdaName);
        handlerArguments.add(methodName);

        JCTree.JCLiteral variableName = getJavac().literal(variable.toString());
        handlerArguments.add(variableName);
        handlerArguments.add(variable);

        JCTree.JCExpressionStatement call = getJavac().call(valueChangeHandlerExpression, handlerArguments.toList());

        Logger.note(getClass(), "valueChangeExpression", call.toString());
        return call;
    }

}
