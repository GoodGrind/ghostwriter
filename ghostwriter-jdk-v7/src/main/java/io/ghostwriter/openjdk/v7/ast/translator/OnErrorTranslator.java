package io.ghostwriter.openjdk.v7.ast.translator;


import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import io.ghostwriter.openjdk.v7.ast.compiler.JavaCompiler;
import io.ghostwriter.openjdk.v7.ast.compiler.JavaCompilerHelper;
import io.ghostwriter.openjdk.v7.common.Logger;
import io.ghostwriter.openjdk.v7.model.Method;

import java.util.Iterator;
import java.util.Objects;

/**
 * Depends on EnteringExitingTranslator, that creates the try-finally block used by OnErrorTranslator
 */
public class OnErrorTranslator implements Translator<Method> {

    private final JavaCompiler javac;

    private final JavaCompilerHelper compilerHelper;

    public OnErrorTranslator(JavaCompiler javac, JavaCompilerHelper helper) {
        this.javac = Objects.requireNonNull(javac);
        this.compilerHelper = Objects.requireNonNull(helper);
    }

    @Override
    public void translate(Method model) {
        JCTree.JCMethodDecl representation = model.representation();

        ListBuffer<JCTree.JCStatement> instrumentedBody = new ListBuffer<>();

        instrumentExceptionCapture(model, instrumentedBody);

        representation.body.stats = instrumentedBody.toList();
    }

    private void instrumentExceptionCapture(Method model, ListBuffer<JCTree.JCStatement> instrumentedBody) {
        JCTree.JCMethodDecl representation = model.representation();
        Iterator<JCTree.JCStatement> topLevelStatements = representation.body.stats.iterator();

        // We only go through the top level statements since we expect a specific method structure created by EnteringExitingTranslator
        // OnErrorTranslator depends on the method body structure created by EnteringExitingTranslator,
        // since we assume that there is a top level try-finally statement that we extend.
        boolean foundEnteringExitingTryFinallyBlock = false;
        while (topLevelStatements.hasNext()) {
            JCTree.JCStatement statement = topLevelStatements.next();
            if (statement instanceof JCTree.JCTry) {
                foundEnteringExitingTryFinallyBlock = true;
                JCTree.JCTry tryFinally = (JCTree.JCTry) statement;
                JCTree.JCTry tryCatchFinally = extendWithErrorCapture(model, tryFinally);
                instrumentedBody.add(tryCatchFinally);
            } else {
                instrumentedBody.add(statement);
            }
        }

        if (!foundEnteringExitingTryFinallyBlock) {
            // If this happens, most likely the EnteringExitingTranslator didn't get executed beforehand or it changed...
            // This way we find out soon enough!
            throw new IllegalArgumentException("Method does not have the expected structure:  " + representation.toString());
        }
    }

    protected JCTree.JCTry extendWithErrorCapture(Method model, JCTree.JCTry tryFinally) {
        JCTree.JCBlock exitingBlock = tryFinally.getFinallyBlock();

        JCTree.JCBlock methodBodyBlock = tryFinally.body;
        JCTree.JCCatch captureException = captureException(model);
        // try-catch-finally setup for supporting error reporting as well
        return javac.tryCatchFinally(methodBodyBlock, captureException, exitingBlock);
    }

    protected String getExecutionStatusHandler() {
        return io.ghostwriter.openjdk.v7.common.RuntimeHandler.ON_ERROR.toString();
    }

    protected JCTree.JCCatch captureException(Method model) {
        String caughtExceptionParameterName = catchBlockParameterName(model);

        JCTree.JCIdent capturedExceptionIdentifier = javac.identifier(caughtExceptionParameterName);

        final JCTree.JCStatement onErrorExpression = onErrorExpression(model);
        JCTree.JCStatement throwStatement = javac.throwStatement(capturedExceptionIdentifier);
        final List<JCTree.JCStatement> statements = List.of(onErrorExpression, throwStatement);
        JCTree.JCBlock catchBlock = javac.block(statements);

        final JCTree.JCVariableDecl catchParameter =
                javac.catchParameter(caughtExceptionParameterName, model.representation());

        return javac.catchExpression(catchParameter, catchBlock);
    }

    protected String catchBlockParameterName(Method model) {
        // FIXME(snorbi07): it wouldn't hurt to check whether we shadow something
        // See EnteringExitingTranslator result variable name generation fixme for additional hints .
        return "$e_";
    }

    protected JCTree.JCStatement onErrorExpression(Method model) {
        String onErrorHandler = getExecutionStatusHandler();
        if (onErrorHandler == null || "".equals(onErrorHandler)) {
            Logger.error(getClass(), "onErrorExpression", "invalid fully qualified name for 'onError' handler: " + String.valueOf(onErrorHandler));
        }

        JCTree.JCExpression exitingHandlerExpression = javac.expression(onErrorHandler);
        ListBuffer<JCTree.JCExpression> handlerArguments = new ListBuffer<>();

        JCTree.JCExpression thisOrClass = compilerHelper.methodContext(model);
        handlerArguments.add(thisOrClass);

        JCTree.JCLiteral methodName = compilerHelper.methodName(model);
        handlerArguments.add(methodName);

        String exceptionVariableName = catchBlockParameterName(model);
        JCTree.JCExpression exceptionVariableIdentifier = javac.identifier(exceptionVariableName);
        handlerArguments.add(exceptionVariableIdentifier);

        return javac.call(exitingHandlerExpression, handlerArguments.toList());
    }

}