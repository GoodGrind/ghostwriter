package io.ghostwriter.openjdk.v7.ast.translator;


import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import io.ghostwriter.openjdk.v7.ast.compiler.JavaCompiler;
import io.ghostwriter.openjdk.v7.ast.compiler.JavaCompilerHelper;
import io.ghostwriter.openjdk.v7.common.Logger;
import io.ghostwriter.openjdk.v7.model.Method;

import java.util.Objects;

public class MethodTranslator implements Translator<Method> {

    private final JavaCompiler javac;

    private final JavaCompilerHelper helper;
    
    public MethodTranslator(JavaCompiler javac, JavaCompilerHelper helper) {
        this.javac = Objects.requireNonNull(javac, "Must provide a valid instance of " + JavaCompiler.class.getSimpleName());
        this.helper = Objects.requireNonNull(helper, "Must provide a valid instance of " + JavaCompilerHelper.class.getSimpleName());
    }

    protected boolean doTraceValueChanges() {
        // Here only for extensibility reasons. Value change tracking feature is optional.
        return true;
    }

    protected boolean doTraceErrors() {
        // Here only for extensibility reasons. Error tracking feature is optional.
        return true;
    }

    protected boolean doCaptureTimeouts(Method model) {
        final JCMethodDecl methodDecl = model.representation();
        final JCTree.JCAnnotation timeoutAnnotation = helper.getTimeoutAnnotation(methodDecl);

        return timeoutAnnotation != null;
    }

    /**
     * After instrumentation, the method will have a structure like one presented here:
     *
     * GW.enteringStatement(...)
     * try {
     * ... original method body
     * ... GW.returning(...)
     * }
     * catch (Throwable t) {
     * GW.onError(...);
     * }
     * finally {
     * GW.exitingStatement(...)
     * }
     *
     * @param model - method that will be instrumented
     */
    @Override
    public void translate(Method model) {
        JCMethodDecl representation = model.representation();

        // if the method or the class that it belongs to is annotated with the GW Exclude annotation then we ignore it
        boolean isExcluded = isMethodExcluded(model);
        if (isExcluded) {
            Logger.note(getClass(), "translate", "skipping instrumentation of method: " + model.getName());
            return;
        }

        boolean isDefined = representation.body != null;
        if (!isDefined) {
            // abstract methods do not have a body for example, if no method body is present, we skip the instrumentation
            return;
        }

        // ensure that various transformation steps 
        transformToBlockConstructs(model);

        // first and foremost we have to do the value tracing, otherwise all other instrumentation codes could be affected, since this step really does instrumentation all over the place
        // NOTE: possible workaround to this restriction is to only apply the value change instrumentation to the original method body and not to the instrumented one.
        if (doTraceValueChanges()) {
            // we modify the body of the method to trace each assignment operator (for sure that is a value change!)
            traceValueChanges(model);
        }

        // modify the method body to have an entering and exiting call triggered 
        traceEnteringExiting(model);

        // modify the method body to capture return calls
        traceReturn(model);

        // in case of annotated methods, trace timeout events as well
        if (doCaptureTimeouts(model)) {
            traceTimeout(model);
        }

        // modify the method body to capture and trace unexpected errors
        if (doTraceErrors()) {
            traceErrors(model);
        }

        // if we are dealing with a constructor, we must ensure that the 1st statement of the body is the
        // original call to super(...) or this(...)
        if (helper.isConstructor(representation)) {
            transformConstructor(model);
        }

        // we print out the instrumented code for debugging purposes
        String fullyQualifiedClassName = model.getClazz().getFullyQualifiedClassName();
        Logger.note(getClass(), "translate", "(" + fullyQualifiedClassName + ")" + representation.toString());
    }

    protected void traceEnteringExiting(Method model) {
        Translator<Method> enteringExitingTranslator = new EnteringExitingTranslator(javac, helper);
        enteringExitingTranslator.translate(model);
    }

    protected void traceReturn(Method model) {
        final ReturningTranslator returningTranslator = new ReturningTranslator(javac, helper);
        returningTranslator.translate(model);
    }

    protected void traceTimeout(Method model) {
        final TimeoutTranslator timeoutTranslator = new TimeoutTranslator(javac, helper);
        timeoutTranslator.translate(model);
    }

    protected void traceErrors(Method model) {
        Translator<Method> onErrorTranslator = new OnErrorTranslator(javac, helper);
        onErrorTranslator.translate(model);
    }

    /**
     * Destructive procedure that modifies the method body to contain a 'change' event signal after each assignment operator.
     * The new assigned value and the right-hand-side receiver name are sent to the runtime component.
     * Assignment tracking is done after the assignment expression executed successfully.
     *
     * @param model - method representation that gets modified
     */
    protected void traceValueChanges(Method model) {
        ReturnExpressionMutationExtractionTranslator returnExpressionTranslator = new ReturnExpressionMutationExtractionTranslator(javac, helper);
        returnExpressionTranslator.translate(model);
        ValueChangeTranslator valueChangeTranslator = new ValueChangeTranslator(javac, helper);
        valueChangeTranslator.translate(model);
    }

    protected void transformConstructor(Method model) {
        // extract constructor calls as first statements
        ConstructorTranslator constructorTranslator = new ConstructorTranslator(helper);
        constructorTranslator.translate(model);
    }

    protected void transformToBlockConstructs(Method model) {
        // ensure that all if,for,while,foreach,... constructs are using a block instead of a single expression
        // otherwise adding the necessary API calls can lead to syntax errors
        WrapInBlockTranslator wrapInBlockTranslator = new WrapInBlockTranslator(javac);
        wrapInBlockTranslator.translate(model);
    }

    protected boolean isMethodExcluded(Method model) {
        final JCMethodDecl methodRepresentation = model.representation();
        final boolean isMethodExcluded = helper.isExcluded(methodRepresentation);
        final JCTree.JCClassDecl classRepresentation = model.getClazz().representation();
        final boolean isClassExcluded = helper.isExcluded(classRepresentation);

        // a method should be skipped if the class it belongs to has an Exclude annotation
        // or the method itself was annotated with Exclude.
        return isClassExcluded || isMethodExcluded;
    }

    protected JavaCompiler getJavac() {
        return javac;
    }

    protected JavaCompilerHelper getHelper() {
        return helper;
    }

}
