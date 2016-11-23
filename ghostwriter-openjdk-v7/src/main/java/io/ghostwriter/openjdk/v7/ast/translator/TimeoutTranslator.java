package io.ghostwriter.openjdk.v7.ast.translator;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import io.ghostwriter.openjdk.v7.ast.compiler.JavaCompiler;
import io.ghostwriter.openjdk.v7.ast.compiler.JavaCompilerHelper;
import io.ghostwriter.openjdk.v7.common.Logger;
import io.ghostwriter.openjdk.v7.common.RuntimeHandler;
import io.ghostwriter.openjdk.v7.model.Method;

import java.util.Iterator;
import java.util.Objects;


public class TimeoutTranslator implements Translator<Method> {

    private final JavaCompiler javac;

    private final JavaCompilerHelper helper;

    public TimeoutTranslator(JavaCompiler javac, JavaCompilerHelper helper) {
        this.javac = Objects.requireNonNull(javac);
        this.helper = Objects.requireNonNull(helper);
    }

    @Override
    public void translate(Method model) {
        final long timeoutThreshold = getTimeoutThreshold(model);
        instrumentStartMeasurement(model);
        instrumentStopMeasurement(model, timeoutThreshold);
    }

    private void instrumentStartMeasurement(Method model) {
        final String startTimestampVariableName = getStartTimestampVariableName(model);
        final JCTree.JCVariableDecl startTimestamp = captureTimeStamp(startTimestampVariableName, model);

        ListBuffer<JCTree.JCStatement> newBody = new ListBuffer<>();
        newBody.add(startTimestamp);

        final JCTree.JCMethodDecl methodDecl = model.representation();
        appendStatements(newBody, methodDecl.body);

        methodDecl.body.stats = newBody.toList();
    }

    private void instrumentStopMeasurement(Method model, long timeoutThreshold) {
        ListBuffer<JCTree.JCStatement> newBody = new ListBuffer<>();

        final String stopTimestampVariableName = getStopTimestampVariableName(model);
        final JCTree.JCVariableDecl stopTimestamp = captureTimeStamp(stopTimestampVariableName, model);
        newBody.add(stopTimestamp);

        final JCTree.JCStatement deltaCheck = timestampDeltaCheck(model, timeoutThreshold);
        newBody.add(deltaCheck);

        final JCTree.JCMethodDecl methodDecl = model.representation();
        final JCTree.JCTry enteringExitingTryConstruct = findEnteringExitingTryConstruct(methodDecl);
        final JCTree.JCBlock finallyBlock = enteringExitingTryConstruct.getFinallyBlock();
        appendStatements(newBody, finallyBlock);

        enteringExitingTryConstruct.finalizer.stats = newBody.toList();
    }

    private JCTree.JCStatement timestampDeltaCheck(Method model, long timeoutThreshold) {
        final String startTimestampVariableName = getStartTimestampVariableName(model);
        final String stopTimestampVariableName = getStopTimestampVariableName(model);

        final JCTree.JCIdent stopIdentifier = javac.identifier(stopTimestampVariableName);
        final JCTree.JCIdent startIdentifier = javac.identifier(startTimestampVariableName);
        final JCTree.JCBinary measurementDelta = javac.binary("-", stopIdentifier, startIdentifier);

        final JCTree.JCBinary deltaCondition = javac.binary("<", javac.literal(timeoutThreshold), measurementDelta);

        final JCTree.JCStatement deltaCheck = javac.ifCondition(deltaCondition, timeoutHandlerBlock(model, timeoutThreshold, measurementDelta));

        return deltaCheck;
    }

    private JCTree.JCBlock timeoutHandlerBlock(Method model, long timeoutThreshold, JCTree.JCBinary measurementDelta) {
        String timeoutHandler = getTimeoutHandler();
        if (timeoutHandler == null || "".equals(timeoutHandler)) {
            Logger.error(getClass(), "timeoutHandlerBlock", "invalid fully qualified name for 'timeout' handler: " + String.valueOf(timeoutHandler));
        }

        JCTree.JCExpression enteringHandlerExpression = javac.expression(timeoutHandler);
        ListBuffer<JCTree.JCExpression> handlerArguments = new ListBuffer<>();

        JCTree.JCExpression thisOrClass = helper.methodContext(model);
        handlerArguments.add(thisOrClass);

        JCTree.JCLiteral methodName = helper.methodName(model);
        handlerArguments.add(methodName);

        handlerArguments.add(javac.literal(timeoutThreshold));

        handlerArguments.add(measurementDelta);

        final JCTree.JCExpressionStatement call = javac.call(enteringHandlerExpression, handlerArguments.toList());

        return javac.block(List.<JCTree.JCStatement>of(call));
    }

    private String getStartTimestampVariableName(Method model) {
        return "_$startTimestamp_" + model.getName();
    }

    private String getStopTimestampVariableName(Method model) {
        return "_$stopTimestamp_" + model.getName();
    }

    private JCTree.JCVariableDecl captureTimeStamp(String timestampVariableName, Method model) {
        final JCTree.JCExpression method = javac.expression("System.currentTimeMillis");
        final JCTree.JCMethodInvocation callCurrentTimeMillis = javac.apply(method, List.<JCTree.JCExpression>nil());
        final JCTree.JCExpression longType = javac.primitiveType("long");

        return javac.finalVariable(longType, timestampVariableName, callCurrentTimeMillis, model.representation());
    }

    private void appendStatements(ListBuffer<JCTree.JCStatement> newBody, JCTree.JCBlock originalBody) {
        final List<JCTree.JCStatement> statements = originalBody.stats;
        for (JCTree.JCStatement statement : statements) {
            newBody.add(statement);
        }
    }

    private JCTree.JCTry findEnteringExitingTryConstruct(JCTree.JCMethodDecl representation) {
        final Iterator<JCTree.JCStatement> statements = representation.body.stats.iterator();

        JCTree.JCTry result = null;
        while (statements.hasNext()) {
            final JCTree.JCStatement currentStatement = statements.next();
            if (currentStatement instanceof JCTree.JCTry) {
                result = (JCTree.JCTry) currentStatement;
                break;
            }
        }

        if (result == null) {
            // If this happens, most likely the EnteringExitingTranslator didn't get executed beforehand or it changed...
            throw new IllegalArgumentException("Missing the entering/exiting block for the method:  " + representation.toString());
        }

        return result;
    }

    private long getTimeoutThreshold(Method model) {
        final JCTree.JCAnnotation timeoutAnnotation = helper.getTimeoutAnnotation(model.representation());
        final List<JCTree.JCExpression> arguments = timeoutAnnotation.getArguments();

        if (arguments.isEmpty()) {
            throw new IllegalStateException("Missing threshold value from Timeout annotation: " + String.valueOf(timeoutAnnotation));
        }
        final int NUMBER_OF_TIMEOUT_ANNOTATION_ATTRIBUTES = 1;
        if (arguments.size() != NUMBER_OF_TIMEOUT_ANNOTATION_ATTRIBUTES) {
            throw new IllegalStateException("Unexpected number of annotation attributes on: " + String.valueOf(timeoutAnnotation));
        }

        final JCTree.JCAssign thresholdExpression = (JCTree.JCAssign) arguments.get(0);

        // NOTE(snorbi07): i'm pretty sure there is a "proper" solution for extracting the literal value from the AST
        final String valueLiteral = thresholdExpression.rhs.toString();

        return Long.parseLong(valueLiteral);
    }

    public String getTimeoutHandler() {
        return RuntimeHandler.TIMEOUT.toString();
    }
}
