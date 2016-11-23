package io.ghostwriter.openjdk.v7.ast.translator;


import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import io.ghostwriter.openjdk.v7.ast.compiler.JavaCompiler;
import io.ghostwriter.openjdk.v7.ast.compiler.JavaCompilerHelper;
import io.ghostwriter.openjdk.v7.common.Logger;
import io.ghostwriter.openjdk.v7.model.Method;
import io.ghostwriter.openjdk.v7.model.Parameter;

import java.util.ArrayList;
import java.util.Objects;

public class EnteringExitingTranslator extends TreeTranslator implements Translator<Method> {

    private static final String ARGUMENTS_ARRAY_TYPE = "java.lang.Object";

    private final JavaCompiler javac;

    private final JavaCompilerHelper helper;

    private Method method;

    private boolean isProcessingNestedClass;
    
    public EnteringExitingTranslator(JavaCompiler javac, JavaCompilerHelper helper) {
        this.javac = Objects.requireNonNull(javac);
        this.helper = Objects.requireNonNull(helper);
        // NOTE(snorbi07): Potential "cleaner" solution would be to move the MethodDeclarationCollector logic here as well.
        this.isProcessingNestedClass = false; // by default we process the methods of a top level class
    }

    @Override
    public void visitMethodDef(JCTree.JCMethodDecl jcMethodDecl) {
        if (!isProcessingNestedClass) {
            final List<JCTree.JCStatement> originalBody = jcMethodDecl.body.getStatements();
            final List<JCTree.JCStatement> instrumentedBody = instrumentedBody(method, originalBody);
            jcMethodDecl.body.stats = instrumentedBody;
        }
        super.visitMethodDef(jcMethodDecl);
    }

    @Override
    public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
        isProcessingNestedClass = true;
        super.visitClassDef(jcClassDecl);
    }

    @Override
    public void translate(Method model) {
        this.method = Objects.requireNonNull(model);
        model.representation().accept(this);
    }

    protected List<JCTree.JCStatement> instrumentedBody(Method model, List<JCTree.JCStatement> originalBody) {
        ListBuffer<JCTree.JCStatement> instrumentedBody = new ListBuffer<>();

        // generate and add the entering expression to the new method body
        JCTree.JCExpressionStatement enteringExpression = enteringExpression(model);
        instrumentedBody.add(enteringExpression);

        //  method body that gets wrapped in a try-catch-finally block to assure entering/exiting runtime event triggering
        JCTree.JCBlock methodBodyBlock = javac.block(originalBody);

        // exiting
        JCTree.JCStatement exitingExpression = exitingExpression(model);
        JCTree.JCBlock exitingBlock = javac.block(List.of(exitingExpression));
        // try-finally setup for method body and exiting statement
        JCTree.JCTry tryBlock = javac.tryFinally(methodBodyBlock, exitingBlock);
        instrumentedBody.add(tryBlock);

        return instrumentedBody.toList();
    }

    protected String getArgumentsArrayType() {
        return ARGUMENTS_ARRAY_TYPE;
    }

    protected String getEnteringHandler() {
        return io.ghostwriter.openjdk.v7.common.RuntimeHandler.ENTERING.toString();
    }

    protected String getExitingHandler() {
        return io.ghostwriter.openjdk.v7.common.RuntimeHandler.EXITING.toString();
    }

    protected JCTree.JCExpressionStatement enteringExpression(Method model) {
        String enteringHandler = getEnteringHandler();
        if (enteringHandler == null || "".equals(enteringHandler)) {
            Logger.error(getClass(), "enteringExpression", "invalid fully qualified name for 'entering' handler: " + String.valueOf(enteringHandler));
        }

        JCTree.JCExpression enteringHandlerExpression = javac.expression(enteringHandler);
        ListBuffer<JCTree.JCExpression> handlerArguments = new ListBuffer<>();

        JCTree.JCExpression thisOrClass = helper.methodContext(model);
        handlerArguments.add(thisOrClass);

        JCTree.JCLiteral methodName = helper.methodName(model);
        handlerArguments.add(methodName);

        JCTree.JCExpression methodArguments = enteringHandlerParameterArray(model.getParameters());
        handlerArguments.add(methodArguments);

        return javac.call(enteringHandlerExpression, handlerArguments.toList());
    }

    protected JCTree.JCExpressionStatement exitingExpression(Method model) {
        String exitingHandler = getExitingHandler();
        if (exitingHandler == null || "".equals(exitingHandler)) {
            Logger.error(getClass(), "exitingExpression", "invalid fully qualified name for 'exiting' handler: " + String.valueOf(exitingHandler));
        }

        JCTree.JCExpression exitingHandlerExpression = javac.expression(exitingHandler);
        ListBuffer<JCTree.JCExpression> handlerArguments = new ListBuffer<>();

        JCTree.JCExpression thisOrClass = helper.methodContext(model);
        handlerArguments.add(thisOrClass);

        JCTree.JCLiteral methodName = helper.methodName(model);
        handlerArguments.add(methodName);

        return javac.call(exitingHandlerExpression, handlerArguments.toList());
    }

    protected JCTree.JCExpression enteringHandlerParameterArray(java.util.List<Parameter> parameters) {
        ListBuffer<JCTree.JCExpression> lb = new ListBuffer<>();

        final java.util.List<Parameter> filteredParameters = filterExcludedParameters(parameters);
        for (Parameter parameter : filteredParameters) {
            JCTree.JCLiteral argumentName = javac.literal(parameter.getName());
            JCTree.JCExpression argumentValue = argumentExpression(parameter);
            lb.add(argumentName);
            lb.add(argumentValue);
        }

        String argumentsArrayType = getArgumentsArrayType();
        if (argumentsArrayType == null || "".equals(argumentsArrayType)) {
            Logger.error(getClass(), "enteringHandlerParameterArray", "invalid fully qualified name for 'arguments' array type: " + String.valueOf(argumentsArrayType));
        }

        JCTree.JCExpression argumentsArrayTypeExpression = javac.expression(argumentsArrayType);
        JCTree.JCNewArray argumentsArray = javac.array(argumentsArrayTypeExpression);
        argumentsArray.elems = lb.toList();

        return argumentsArray;
    }

    private java.util.List<Parameter> filterExcludedParameters(java.util.List<Parameter> parameters) {
        java.util.List<Parameter> filteredParameters = new ArrayList<>();
        for (Parameter parameter : parameters) {
            final JCTree.JCVariableDecl representation = parameter.representation();
            if (!helper.isExcluded(representation)) {
                filteredParameters.add(parameter);
            }
        }

        return filteredParameters;
    }

    protected JCTree.JCExpression argumentExpression(Parameter parameter) {
        String argName = parameter.getName();

        return javac.expression(argName);
    }

    protected Method getMethod() {
        return method;
    }

    protected void setMethod(Method method) {
        this.method = method;
    }

    protected JavaCompiler getJavac() {
        return javac;
    }

    protected JavaCompilerHelper getJavacHelper() {
        return helper;
    }
}
