package io.ghostwriter.openjdk.v7.ast.collector;

import com.sun.tools.javac.tree.JCTree;
import io.ghostwriter.openjdk.v7.ast.compiler.JavaCompilerHelper;

import java.util.Objects;

public class ConstructorCallCollector extends Collector<JCTree.JCExpressionStatement> {

    private final JavaCompilerHelper helper;

    public ConstructorCallCollector(JavaCompilerHelper helper, JCTree rootElement) {
        super(rootElement);
        this.helper = Objects.requireNonNull(helper);
    }

    @Override
    public void visitExec(JCTree.JCExpressionStatement statement) {
        if (helper.isStatementSuperOrThisCall(statement)) {
            collect(statement);
        }

        super.visitExec(statement);
    }

}
