package io.ghostwriter.openjdk.v7.ast.translator;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
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
        setEnclosingMethod(Objects.requireNonNull(model));
        JCTree.JCMethodDecl representation = enclosingMethod.representation();
        representation.accept(this);
    }

    @Override
    public void visitBlock(JCTree.JCBlock jcBlock) {
        jcBlock.stats = instrumentReturnCapture(jcBlock.stats);
        super.visitBlock(jcBlock);
    }

    @Override
    public void visitCase(JCTree.JCCase tree) {
        tree.stats = instrumentReturnCapture(tree.getStatements());
        super.visitCase(tree);
    }

    @Override
    public void visitNewClass(JCTree.JCNewClass tree) {
        // Skip nested anonymous inner class expressions
        // Annotation class processor is called separately for anonymous inner classes as well
        result = tree;
    }

    @Override
    public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
        // Skip nested class declarations. Since we iterate through all classes those are processed later as well.
        // If we don't skip it at this stage it will be processed twice.
        // TIL that you can define a named class in a method...
        result = jcClassDecl;
    }

    protected List<JCTree.JCStatement> instrumentReturnCapture(List<JCTree.JCStatement> statements) {
        final ListBuffer<JCTree.JCStatement> newBody = new ListBuffer<>();

        for (JCTree.JCStatement statement : statements) {
            // the assumption is that a single statement should only contain a single return call because we don't
            // traverse nested blocks

            final boolean isReturnCall = statement instanceof JCTree.JCReturn;
            if (isReturnCall) {
                final JCTree.JCReturn returnStatement = (JCTree.JCReturn) statement;
                instrumentReturningCall(newBody, returnStatement);
            }

            newBody.add(statement);
        }

        return newBody.toList();
    }

    protected void instrumentReturningCall(ListBuffer<JCTree.JCStatement> newBody, JCTree.JCReturn returnStatement) {
        final boolean isEmptyReturnCall = returnStatement.getExpression() == null;
        if (isEmptyReturnCall) {
            return; // it is a call like this, so there is no result that needs to be captured
        }

        final JCTree.JCVariableDecl capturedResult = captureNestedReturnValue(returnStatement);
        newBody.add(capturedResult);
        final JCTree.JCExpressionStatement returningApiCall = captureMethodResult(capturedResult.getName().toString());
        newBody.add(returningApiCall);
        replaceResultWithCaptureVariable(returnStatement);
    }

    protected void replaceResultWithCaptureVariable(JCTree.JCReturn nestedReturn) {
        final String correspondingCapturedResultName = resultVariableName(nestedReturn);
        final JCTree.JCIdent identifier = javac.identifier(correspondingCapturedResultName);
        nestedReturn.expr = identifier;
    }

    protected JCTree.JCVariableDecl captureNestedReturnValue(JCTree.JCReturn returnStatement) {
        final String resultVariableName = resultVariableName(returnStatement);
        final JCTree.JCVariableDecl captureVar =
                    javac.finalVariable(resultType(), resultVariableName, returnStatement.expr, returnStatement);
        return captureVar;
    }

    protected String resultVariableName(JCTree.JCReturn jcReturn) {
        return "$capturedResult_" + enclosingMethod.getName() + "_" + jcReturn.hashCode();
    }

    protected JCTree.JCExpressionStatement captureMethodResult(String resultCaptureVariable) {
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

        final JCTree.JCExpression returningResult = returningResultExpression(javac.identifier(resultCaptureVariable));
        handlerArguments.add(returningResult);

        return javac.call(returningHandlerExpression, handlerArguments.toList());
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
