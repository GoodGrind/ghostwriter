package io.ghostwriter.openjdk.v7.ast.translator;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.ListBuffer;
import io.ghostwriter.openjdk.v7.ast.compiler.JavaCompiler;
import io.ghostwriter.openjdk.v7.ast.compiler.JavaCompilerHelper;
import io.ghostwriter.openjdk.v7.common.Logger;
import io.ghostwriter.openjdk.v7.model.Method;

import java.util.Objects;

// Instrument GhostWriter.retuning calls in the processed method. Ignores sub-classes.
public class ReturningTranslator extends TreeTranslator implements Translator<Method> {

    private final JavaCompiler javac;

    private final JavaCompilerHelper helper;

    private Method enclosingMethod;

    public ReturningTranslator(JavaCompiler javac, JavaCompilerHelper helper) {
        this.javac = Objects.requireNonNull(javac);
        this.helper = Objects.requireNonNull(helper);
    }

    @Override
    public void translate(Method model) {
        enclosingMethod = Objects.requireNonNull(model);
        JCTree.JCMethodDecl representation = enclosingMethod.representation();
        representation.accept(this);
    }

    @Override
    public void visitNewClass(JCTree.JCNewClass tree) {
        // Skip nested anonymous inner class expressions when collecting return statements.
        // Annotation class processor is called separately for anonymous inner classes as well
        result = tree;
    }

    @Override
    public void visitReturn(JCTree.JCReturn jcReturn) {
        final JCTree.JCExpression expression = jcReturn.getExpression();
        final boolean isEmptyReturnStatement = expression == null; // meaning a simple "return;"
        if (!isEmptyReturnStatement) {
            final JCTree.JCExpression returningExpression = instrumentReturningExpression(expression);
            jcReturn.expr = returningExpression;
        }

        super.visitReturn(jcReturn);
    }

    protected JCTree.JCExpression instrumentReturningExpression(JCTree.JCExpression returnExpression) {
        String returningHandler = getReturningHandler();
        if (returningHandler == null || "".equals(returningHandler)) {
            Logger.error(getClass(), "returningHandler", "invalid fully qualified name for 'exiting' handler: "
                    + String.valueOf(returningHandler));
        }

        final JCTree.JCExpression returningHandlerExpression = javac.expression(returningHandler);
        ListBuffer<JCTree.JCExpression> handlerArguments = new ListBuffer<>();

        final JCTree.JCExpression thisOrClass = returningExpressionContext();
        handlerArguments.add(thisOrClass);

        final JCTree.JCLiteral methodName = returningExpressionMethodName();
        handlerArguments.add(methodName);

        final JCTree.JCExpression returningResult = returningResultExpression(returnExpression);
        handlerArguments.add(returningResult);

        final JCTree.JCExpression resultType = resultType();
        final JCTree.JCMethodInvocation returningCall =
                javac.apply(returningHandlerExpression, handlerArguments.toList());
        final JCTree.JCExpression typeCoercedGwCall = javac.castToType(resultType, returningCall);
        return typeCoercedGwCall;
    }

    protected JCTree.JCExpression resultType() {
        return (JCTree.JCExpression) enclosingMethod.representation().getReturnType();
    }

    protected JCTree.JCExpression returningResultExpression(JCTree.JCExpression returnExpression) {
        final JCTree.JCExpression resultType = resultType();
        if (javac.isPrimitiveType(resultType)) {
            // parameter cast required because of implicit conversions and boxing use cases (int -> Long) can lead to class cast exceptions
            return javac.castToType(resultType, returnExpression);
        }

        return returnExpression;
    }

    protected JCTree.JCExpression returningExpressionContext() {
        return helper.methodContext(enclosingMethod);
    }

    protected JCTree.JCLiteral returningExpressionMethodName() {
        return helper.methodName(enclosingMethod);
    }

    protected String getReturningHandler() {
        return io.ghostwriter.openjdk.v7.common.RuntimeHandler.RETURNING.toString();
    }

    protected Method getEnclosingMethod() {
        return enclosingMethod;
    }

    protected void setEnclosingMethod(Method enclosingMethod) {
        this.enclosingMethod = enclosingMethod;
    }

    protected JavaCompiler getJavac() {
        return javac;
    }

    protected JavaCompilerHelper getHelper() {
        return helper;
    }

}
