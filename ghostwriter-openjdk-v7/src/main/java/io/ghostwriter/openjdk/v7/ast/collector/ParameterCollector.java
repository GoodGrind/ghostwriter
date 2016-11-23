package io.ghostwriter.openjdk.v7.ast.collector;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import io.ghostwriter.openjdk.v7.ast.compiler.JavaCompiler;
import io.ghostwriter.openjdk.v7.common.Logger;
import io.ghostwriter.openjdk.v7.model.Parameter;

import java.util.Objects;

public class ParameterCollector extends Collector<Parameter> {
    
    private final JavaCompiler javac;

    public ParameterCollector(JavaCompiler javac, JCTree rootElement) {
        super(rootElement);
        this.javac = Objects.requireNonNull(javac);
    }

    @Override
    public void visitVarDef(JCVariableDecl varDecl) {
        Parameter parameter = buildParameterModel(varDecl);
        collect(parameter);
        super.visitVarDef(varDecl);
    }

    @Override
    public void visitBlock(JCBlock tree) {
        // skip the method body otherwise we would collect all
        // variable declarations of the method implementation
        result = tree;
    }

    protected Parameter buildParameterModel(JCTree.JCVariableDecl param) {
        assert param.name != null;
        String name = param.name.toString();
        JCTree.JCExpression parameterTypeExpression = param.vartype;
        final JCTree.JCExpression jcExpression = javac.declarationType(parameterTypeExpression);
        final String type = javac.fullyQualifiedNameForTypeExpression(jcExpression);

        Parameter parameter = new Parameter(name, type, param);
        Logger.note(getClass(), "buildParameterModel", parameter.toString());

        return parameter;
    }

}
