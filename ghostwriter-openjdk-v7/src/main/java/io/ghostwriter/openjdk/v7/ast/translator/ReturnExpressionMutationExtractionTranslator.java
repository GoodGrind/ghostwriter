package io.ghostwriter.openjdk.v7.ast.translator;


import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import io.ghostwriter.openjdk.v7.ast.collector.BlockBasedMutatedVariableCollector;
import io.ghostwriter.openjdk.v7.ast.compiler.JavaCompiler;
import io.ghostwriter.openjdk.v7.ast.compiler.JavaCompilerHelper;
import io.ghostwriter.openjdk.v7.model.Method;

import java.util.Objects;
import java.util.UUID;

/**
 * Return statements might contain value assignments or other expression that can trigger value change events.
 * In order to support this language construct, the code needs to be modified in such a way that the return statements
 * only contain the result of the value change expression and not the value change expression itself.
 * Depends on WrapInBlockTranslator execution, since it assumes that 'return' statements will only occur in blocks and
 * not inside other language constructs, such as an 'if'.
 */
public class ReturnExpressionMutationExtractionTranslator extends TreeTranslator implements Translator<Method> {

    final private JavaCompiler javac;

    final private JavaCompilerHelper helper;

    private Method model;

    public ReturnExpressionMutationExtractionTranslator(JavaCompiler javac, JavaCompilerHelper helper) {
        this.javac = javac;
        this.helper = helper;
    }

    @Override
    public void translate(Method model) {
        this.model = Objects.requireNonNull(model);
        JCTree.JCMethodDecl representation = model.representation();
        representation.accept(this);
    }

    @Override
    public void visitBlock(JCTree.JCBlock block) {
        final List<JCTree.JCStatement> statements = block.getStatements();
        final List<JCTree.JCStatement> bodyWithResultCapture = createBodyWithResultCapture(statements);
        block.stats = bodyWithResultCapture;

        super.visitBlock(block);
    }

    @Override
    public void visitCase(JCTree.JCCase caseStatement) {
        final List<JCTree.JCStatement> statements = caseStatement.getStatements();
        final List<JCTree.JCStatement> bodyWithResultCapture = createBodyWithResultCapture(statements);
        caseStatement.stats = bodyWithResultCapture;

        super.visitCase(caseStatement);
    }

    private List<JCTree.JCStatement> createBodyWithResultCapture(List<JCTree.JCStatement> statements) {
        ListBuffer<JCTree.JCStatement> newBody = new ListBuffer<>();
        for (JCTree.JCStatement statement : statements) {
            processStatement(statement, newBody);
        }

        return newBody.toList();
    }

    private void processStatement(JCTree.JCStatement statement, ListBuffer<JCTree.JCStatement> newBody) {
        // We are just looking for top-level return statements here! We assume that nested/multiple statements
        // are in different blocks due to the reliance on WrapInBlockTranslator execution.
        boolean isReturnStatement = statement instanceof JCTree.JCReturn;
        if (!isReturnStatement) {
            newBody.add(statement); // it is not a return statement, no need to do anything else
            return;
        }

        assert isReturnStatement;
        JCTree.JCReturn returnStatement = (JCTree.JCReturn) statement;
        processReturnStatement(returnStatement, newBody);
    }

    private void processReturnStatement(JCTree.JCReturn returnStatement, ListBuffer<JCTree.JCStatement> newBody) {
        if (doesContainMutations(returnStatement)) {
            // If a return expression contains some kind of mutation, then we refactor it.
            // For example: return someArray[++index] gets refactored to:
            // var $capturedReturnExpression_ = someArray[++index];
            // return $capturedReturnExpression_;
            // After value tracing translation this will look like this:
            // var $capturedReturnExpression_ = someArray[++index];
            // GW.valueChange(..., index);
            // return $capturedReturnExpression_;
            // So the value change event will be triggered before the return statement and not cause a compilation error by coming after the return statement.
            // For details see the issue #51.
            final String generatedCaptureName = generateMutationCaptureVariableName();
            JCTree.JCExpression expression = returnStatement.getExpression();
            JCTree.JCVariableDecl mutationResult = mutationCaptureVariable(expression, generatedCaptureName);
            // NOTE(snorbi07): currently we modify the original return statement tree element, creating a new one might be a better solution
            returnStatement.expr = javac.expression(generatedCaptureName);
            newBody.add(mutationResult);
        }

        newBody.add(returnStatement);
    }

    private boolean doesContainMutations(JCTree.JCReturn returnStatement) {
        java.util.List<JCTree.JCExpression> valueChangeExpressions =
                new BlockBasedMutatedVariableCollector(returnStatement, javac, helper).toList();
        return !valueChangeExpressions.isEmpty();
    }

    protected JCTree.JCVariableDecl mutationCaptureVariable(JCTree.JCExpression value, String generatedCaptureName) {
        JCTree.JCMethodDecl method = model.representation();

        if (!helper.hasResult(method)) {
            throw new IllegalArgumentException("Method does not have a return type: " + method.toString());
        }

        final JCTree.JCExpression returnType = javac.methodReturnType(method);
        JCTree.JCVariableDecl variable = javac.finalVariable(returnType, generatedCaptureName, value, method);
        JCTree.JCAnnotation excludeAnnotation = javac.annotation("io.ghostwriter.annotation.Exclude");
        variable.mods.annotations = List.of(excludeAnnotation);

        return variable;
    }

    private String generateMutationCaptureVariableName() {
        final String uuidPart = UUID.randomUUID().toString().replaceAll("-", "_");
        return "$capturedReturnExpression_" + uuidPart;
    }
}
