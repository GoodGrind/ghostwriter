package io.ghostwriter.openjdk.v7.ast.translator;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import io.ghostwriter.openjdk.v7.ast.collector.BlockBasedMutatedVariableCollector;
import io.ghostwriter.openjdk.v7.ast.collector.Collector;
import io.ghostwriter.openjdk.v7.ast.compiler.JavaCompiler;
import io.ghostwriter.openjdk.v7.ast.compiler.JavaCompilerHelper;
import io.ghostwriter.openjdk.v7.common.Logger;
import io.ghostwriter.openjdk.v7.model.Method;

import java.util.Iterator;
import java.util.Objects;

public class ValueChangeTranslator extends TreeTranslator implements Translator<Method> {

    private final JavaCompiler javac;

    private final JavaCompilerHelper helper;

    private Method processedMethod;

    public ValueChangeTranslator(JavaCompiler javac, JavaCompilerHelper helper) {
        this.javac = javac;
        this.helper = Objects.requireNonNull(helper);
    }

    @Override
    public void translate(Method model) {
        processedMethod = Objects.requireNonNull(model);
        JCTree.JCMethodDecl representation = processedMethod.representation();
        representation.accept(this);
    }

    @Override
    public void visitBlock(JCTree.JCBlock block) {
        //  We have to hijack during block visiting, since this is where we can append instructions for value tracking.
        // The tree traversal API only allows in-place replacement
        captureValueChanges(block);
        super.visitBlock(block);
        result = block;
    }

    @Override
    public void visitForLoop(JCTree.JCForLoop forLoop) {
        assert forLoop.body instanceof JCTree.JCBlock;
        JCTree.JCBlock body = (JCTree.JCBlock) forLoop.body; // with the call to 'WrapInBlockTranslator' we assure that it is a JCBlock
        List<JCTree.JCExpression> forLoopVariables = collectForLoopUpdateSectionMutatedVariables(forLoop);
        captureForLoopVariableChanges(forLoopVariables, body);
        super.visitForLoop(forLoop);
    }

    @Override
    public void visitForeachLoop(JCTree.JCEnhancedForLoop forEachLoop) {
        assert forEachLoop.body instanceof JCTree.JCBlock;
        JCTree.JCBlock body = (JCTree.JCBlock) forEachLoop.body; // with the call to 'WrapInBlockTranslator' we assure that it is a JCBlock
        JCTree.JCVariableDecl variable = forEachLoop.getVariable();
        Name name = variable.getName();
        final JCTree.JCExpression variableExpression = javac.expression(name.toString());

        captureForLoopVariableChanges(List.of(variableExpression), body);
        super.visitForeachLoop(forEachLoop);
    }

    private void captureForLoopVariableChanges(List<JCTree.JCExpression> forLoopVariables, JCTree.JCBlock forBlock) {
        for (JCTree.JCExpression variable : forLoopVariables) {
            // iterations build on the previously instrumented for loop body. this way after the iteration, the instrumented body should contain the GW tracking for all variables (in reverse order).
            prependValueChangeExpression(variable, forBlock);
        }
    }

    private void prependValueChangeExpression(JCTree.JCExpression variable, JCTree.JCBlock existingBlock) {
        ListBuffer<JCTree.JCStatement> instrumentedForBody = new ListBuffer<>();
        Iterator<JCTree.JCStatement> originalStatements = existingBlock.getStatements().iterator();

        // create a value change expression for the variable
        JCTree.JCExpressionStatement valueChangeExpression = valueChangeExpression(processedMethod, variable);

        // add the loop variable tracking statement as the first statement of the instrumented body
        instrumentedForBody.add(valueChangeExpression);

        // copy the existing body
        while (originalStatements.hasNext()) {
            instrumentedForBody.add(originalStatements.next());
        }

        existingBlock.stats = instrumentedForBody.toList();
    }

    private List<JCTree.JCExpression> collectForLoopUpdateSectionMutatedVariables(JCTree.JCForLoop forLoop) {
        final List<JCTree.JCExpressionStatement> updateStatements = forLoop.getUpdate();

        final ListBuffer<JCTree.JCStatement> statements = new ListBuffer<>();

        for (JCTree.JCExpressionStatement jcExpressionStatement : updateStatements) {
            statements.add(jcExpressionStatement);
        }

        final List<JCTree.JCStatement> jcStatements = statements.toList();

        return extractForLoopVariableNames(forLoop, jcStatements);
    }

    private List<JCTree.JCExpression> extractForLoopVariableNames(JCTree.JCForLoop forLoop, List<JCTree.JCStatement> statements) {
        ListBuffer<JCTree.JCExpression> forLoopVariables = new ListBuffer<>();

        for (JCTree.JCStatement statement : statements) {
            Iterator<JCTree.JCExpression> variables = new BlockBasedMutatedVariableCollector(statement, javac, helper).iterator();
            if (!variables.hasNext()) {
                throw new IllegalStateException("No variable declaration found for initializer statement: " + forLoop.toString());
            }
            JCTree.JCExpression variable = variables.next();
            if (variables.hasNext()) {
                throw new IllegalStateException("Multiple variable declarations found in initializer statement: " + forLoop.toString());
            }
            forLoopVariables.add(variable);
        }
        return forLoopVariables.toList();
    }

    @Override
    public void visitNewClass(JCTree.JCNewClass tree) {
        // Skip nested anonymous inner class expressions when collecting assignment statements.
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

    private void captureValueChanges(JCTree.JCBlock block) {
        List<JCTree.JCStatement> stats = block.stats;
        block.stats = instrumentValueChangeTracing(stats);
    }

    private List<JCTree.JCStatement> instrumentValueChangeTracing(List<JCTree.JCStatement> statements) {
        ListBuffer<JCTree.JCStatement> instrumentedBody = new ListBuffer<>();

        for (JCTree.JCStatement statement : statements) {
            // copy the original body
            instrumentedBody.add(statement);

            // find all assignment operators that should be traced after this statement is executed
            Collector<JCTree.JCExpression> valueChangeCollector = new BlockBasedMutatedVariableCollector(statement, javac, helper);
            for (JCTree.JCExpression variable : valueChangeCollector) {
                captureValueChange(instrumentedBody, variable);
            }
        }

        return instrumentedBody.toList();
    }

    private void captureValueChange(ListBuffer<JCTree.JCStatement> instrumentedBody, JCTree.JCExpression variable) {
        JCTree.JCExpressionStatement valueChangeExpression = valueChangeExpression(processedMethod, variable);
        instrumentedBody.add(valueChangeExpression);
    }

    protected String getValueChangeHandler() {
        return io.ghostwriter.openjdk.v7.common.RuntimeHandler.VALUE_CHANGE.toString();
    }

    protected JCTree.JCExpressionStatement valueChangeExpression(Method model, JCTree.JCExpression variable) {
        String valueChangeHandler = getValueChangeHandler();
        if (valueChangeHandler == null || "".equals(valueChangeHandler)) {
            Logger.error(getClass(), "valueChangeExpression", "invalid fully qualified name for 'valueChange' handler: " + String.valueOf(valueChangeHandler));
        }

        JCTree.JCExpression valueChangeHandlerExpression = javac.expression(valueChangeHandler);
        ListBuffer<JCTree.JCExpression> handlerArguments = new ListBuffer<>();

        JCTree.JCExpression thisOrClass = helper.methodContext(model);
        handlerArguments.add(thisOrClass);

        JCTree.JCLiteral methodName = helper.methodName(model);
        handlerArguments.add(methodName);

        JCTree.JCLiteral variableName = javac.literal(variable.toString());
        handlerArguments.add(variableName);
        handlerArguments.add(variable);

        JCTree.JCExpressionStatement call = javac.call(valueChangeHandlerExpression, handlerArguments.toList());

        Logger.note(getClass(), "valueChangeExpression", call.toString());
        return call;
    }

    @Override
    public void visitCase(JCTree.JCCase tree) {
        List<JCTree.JCStatement> statements = tree.getStatements();
        tree.stats = instrumentValueChangeTracing(statements);

        super.visitCase(tree);
    }

    @Override
    public void visitTry(JCTree.JCTry tryExpression) {
        List<? extends JCTree> resources = tryExpression.getResources();
        if (!resources.isEmpty()) {
            captureResourceInitializations(resources, tryExpression);
        }

        super.visitTry(tryExpression);
    }

    private void captureResourceInitializations(List<? extends JCTree> resources, JCTree.JCTry tryExpression) {
        ListBuffer<JCTree.JCStatement> instrumentedBody = new ListBuffer<>();
        // first add value change tracking for all used resources
        for (JCTree resource : resources) {
            captureResourceInitialization(resource, instrumentedBody);
        }

        // after resource value change tracking events, just copy the original body
        JCTree.JCBlock block = tryExpression.getBlock();
        List<JCTree.JCStatement> originalBlock = block.getStatements();
        for (JCTree.JCStatement statement : originalBlock) {
            instrumentedBody.add(statement);
        }

        // and don't forget to override the original body!
        block.stats = instrumentedBody.toList();
    }

    private void captureResourceInitialization(JCTree resource, ListBuffer<JCTree.JCStatement> instrumentedBody) {
        BlockBasedMutatedVariableCollector resourceMutationCollector = new BlockBasedMutatedVariableCollector(resource, javac, helper);
        for (JCTree.JCExpression mutation : resourceMutationCollector) {
            captureValueChange(instrumentedBody, mutation);
        }
    }

    protected JavaCompiler getJavac() {
        return javac;
    }

    protected JavaCompilerHelper getHelper() {
        return helper;
    }

}


