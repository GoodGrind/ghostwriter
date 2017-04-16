package io.ghostwriter.openjdk.v7.ast.collector;


import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import io.ghostwriter.openjdk.v7.ast.compiler.JavaCompiler;
import io.ghostwriter.openjdk.v7.ast.compiler.JavaCompilerHelper;

import java.util.List;
import java.util.Objects;


/**
 * Collect all variable names that have their value changed in the current block. This can happen through initialization, assignment ...
 */
public class BlockBasedMutatedVariableCollector extends Collector<JCTree.JCExpression> {

    final private JavaCompiler javac;

    final private JavaCompilerHelper helper;

    public BlockBasedMutatedVariableCollector(JCTree rootElement, JavaCompiler javac, JavaCompilerHelper helper) {
        super(rootElement);
        this.javac = Objects.requireNonNull(javac);
        this.helper = Objects.requireNonNull(helper);
    }

    @Override
    public void visitAssign(JCAssign assignment) {
        // super call comes first in order to ensure the correct sequence of valueChange triggers
        super.visitAssign(assignment);
        final JCTree.JCExpression assignmentVariable = assignment.getVariable();
        if (doesContainMutation(assignmentVariable)) {
            // for example in case of an array access we only need the array[index] combination instead of array[++index]
            // so we can pass it to the GW.valueChange hook without producing side effects and altering the original behaviour
            collect(extractAssignmentTarget(assignmentVariable));
        }
        else {
            collect(assignmentVariable);
        }
    }

    private JCTree.JCExpression extractAssignmentTarget(JCTree.JCExpression assignmentVariable) {
        if (!(assignmentVariable instanceof JCTree.JCArrayAccess)) {
            // Fail fast if we stumble upon an unexpected case since we don't know what kind of side effects it would have
            throw new IllegalArgumentException("Unsupported argument: "
                    + assignmentVariable.getClass().getCanonicalName() + ": "
                    + String.valueOf(assignmentVariable));
        }

        final JCTree.JCArrayAccess arrayAccess = (JCTree.JCArrayAccess) assignmentVariable;
        final JCTree.JCExpression index = arrayAccess.getIndex();
        final boolean isAssignmentOperator = index instanceof JCTree.JCAssignOp; // += -= ...

        JCTree.JCExpression sideEffectFreeIndex;
        if (isPostIncrementOperator(index)) {
            JCTree.JCUnary op = (JCTree.JCUnary) index;
            sideEffectFreeIndex = javac.binary("-", op.arg, javac.literal(1));
        }
        else if (isPostDecrementOperator(index)) {
            JCTree.JCUnary op = (JCTree.JCUnary) index;
            sideEffectFreeIndex = javac.binary("+", op.arg, javac.literal(1));
        }
        else if (isPreIncrementOperator(index) || isPreDecrementOperator(index)) {
            JCTree.JCUnary op = (JCTree.JCUnary) index;
            sideEffectFreeIndex = op.arg;
        }
        else if (isAssignmentOperator) {
            JCTree.JCAssignOp assignOp = (JCTree.JCAssignOp) index;
            sideEffectFreeIndex = assignOp.getVariable();
        }
        else {
            // fail fast if we stumble upon something unexpected
            throw new IllegalArgumentException("Unsupported expression type " + index.getClass().getCanonicalName()
                    + ": " + String.valueOf(arrayAccess));
        }

        return javac.arrayAccess(arrayAccess.indexed, sideEffectFreeIndex);
    }

    private boolean doesContainMutation(JCTree.JCExpression expression) {
        final List<JCTree.JCExpression> mutations = new BlockBasedMutatedVariableCollector(expression, javac, helper).toList();
        return !mutations.isEmpty();
    }

    @Override
    public void visitAssignop(JCTree.JCAssignOp assignment) {
        // Capture operators such as +=
        final JCTree.JCExpression variable = assignment.getVariable();
        collect(variable);
        super.visitAssignop(assignment);
    }

    @Override
    public void visitNewClass(JCNewClass tree) {
        // Skip nested anonymous inner class expressions when collecting assignment statements.
        // Annotation class processor is called separately for anonymous inner classes as well
        result = tree;
    }

    @Override
    public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
        // Skip nested class definitions, those are traversed separately.
        result = jcClassDecl;
    }

    @Override
    public void visitVarDef(JCTree.JCVariableDecl variableDecl) {
        JCTree.JCExpression initializer = variableDecl.getInitializer();
        boolean isDeclarationOnly = initializer == null;
        boolean isExcluded = helper.isExcluded(variableDecl);

        // If a variable is declared only, meaning there is no initialization, then there is nothing to capture.
        // For example: int a;
        // Trying to capture this statement would produce a compilation error because we would be referring to an uninitialized variable.
        if (!isDeclarationOnly && !isExcluded) {
            String variable = variableDecl.getName().toString();
            final JCTree.JCExpression variableExpression = javac.expression(variable);
            collect(variableExpression);
        }
        super.visitVarDef(variableDecl);
    }

    @Override
    public void visitForLoop(JCTree.JCForLoop jcForLoop) {
        // for body blocks are considered nested scope as well
        result = jcForLoop;
    }

    @Override
    public void visitForeachLoop(JCTree.JCEnhancedForLoop forEachLoop) {
        // foreach body blocks are considered nested scope as well
        result = forEachLoop;
    }

    @Override
    public void visitCatch(JCTree.JCCatch jcCatch) {
        // try-catch-finally blocks fall into the category of nested scopes as well
        result = jcCatch;
    }

    @Override
    public void visitBlock(JCTree.JCBlock tree) {
        // Ignore nested blocks since those represent a different scope. Nested block are handled when we travers them.
        // Of course, we can refer to parent scopes
        result = tree;
    }

    @Override
    public void visitUnary(JCTree.JCUnary tree) {
        if (isMutableUnaryOperator(tree)) {
            final JCTree.JCExpression expression = tree.getExpression();
            collect(expression);
        }

        super.visitUnary(tree);
    }

    protected boolean isMutableUnaryOperator(JCTree.JCExpression unary) {
        final String expression = unary.toString();
        final boolean doesContainIncrementOp = expression.contains("++");
        final boolean doesContainDecrementOp = expression.contains("--");

        return doesContainDecrementOp || doesContainIncrementOp;
    }

    protected boolean isPostIncrementOperator(JCTree.JCExpression unary) {
        if (!(unary instanceof JCTree.JCUnary)) {
            return false;
        }
        final String expression = unary.toString();

        return expression.endsWith("++");
    }

    protected boolean isPostDecrementOperator(JCTree.JCExpression unary) {
        if (!(unary instanceof JCTree.JCUnary)) {
            return false;
        }
        final String expression = unary.toString();

        return expression.endsWith("--");
    }

    protected boolean isPreIncrementOperator(JCTree.JCExpression unary) {
        if (!(unary instanceof JCTree.JCUnary)) {
            return false;
        }
        final String expression = unary.toString();

        return expression.startsWith("++");
    }

    protected boolean isPreDecrementOperator(JCTree.JCExpression unary) {
        if (!(unary instanceof JCTree.JCUnary)) {
            return false;
        }
        final String expression = unary.toString();

        return expression.startsWith("--");
    }

    @Override
    public void visitSwitch(JCTree.JCSwitch tree) {
        // Ignore switch statements, since case blocks represent a different scope. Nested block are handled when we travers them.
        result = tree;
    }

    @Override
    public void visitIf(JCTree.JCIf tree) {
        // 'if' statements can contain assignments that are short circuited, which can lead to compilation failure.
        // For details check issue #55.
        result = tree;
    }

    @Override
    public void visitTry(JCTree.JCTry tree) {
        // try-with expressions introduced with Java 7 contain an assignment that is only available in a nested scope.
        // For this reason try-with expressions are handled similarly to other scope creating constructs such as 'for' loops.
        result = tree;
    }
}
