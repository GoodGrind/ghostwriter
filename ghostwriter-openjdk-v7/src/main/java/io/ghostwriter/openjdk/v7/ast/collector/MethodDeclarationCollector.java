package io.ghostwriter.openjdk.v7.ast.collector;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import io.ghostwriter.openjdk.v7.ast.compiler.JavaCompiler;
import io.ghostwriter.openjdk.v7.common.Logger;
import io.ghostwriter.openjdk.v7.model.Clazz;
import io.ghostwriter.openjdk.v7.model.Method;
import io.ghostwriter.openjdk.v7.model.Parameter;

import java.util.List;
import java.util.Objects;

public class MethodDeclarationCollector extends Collector<Method> {

    private final JavaCompiler javac;

    public MethodDeclarationCollector(JavaCompiler javac, JCTree.JCClassDecl rootClass) {
        super(rootClass);
        this.javac = Objects.requireNonNull(javac);
    }

    @Override
    public void visitClassDef(JCTree.JCClassDecl klass) {
        collectMethodDefinitions(klass);
        super.visitClassDef(klass);
    }

    /**
     * We use this method instead of overriding the visitMethodDef method because we only want to collect
     * methods belonging to the current class definition. This is required in order to have a correct Clazz model
     * in the Method model instances.
     * Nested classes definitions and their methods are handled when the visitor pattern calls the corresponding
     * visitClassDef handler for those as well.
     *
     * @param klass class repersentation to traverse
     */
    protected void collectMethodDefinitions(JCTree.JCClassDecl klass) {
        for (JCTree member : klass.getMembers()) {
            if (!(member instanceof JCMethodDecl)) {
                continue;
            }
            
            JCMethodDecl methodDecl = (JCMethodDecl) member;
            Method method = buildMethodModel(klass, methodDecl);
            collect(method);
        }
    }
    
    protected Method buildMethodModel(JCTree.JCClassDecl klass, JCMethodDecl methodDeclaration) {
        ParameterCollector parameterCollector = new ParameterCollector(javac, methodDeclaration);
        List<Parameter> parameters = parameterCollector.toList();
        String methodName = javac.methodName(methodDeclaration);

        final Clazz enclosingClass = new Clazz(klass);
        Method method = new Method(methodName, enclosingClass, parameters, methodDeclaration);
        Logger.note(getClass(), "buildMethodModel", method.toString());
        return method;
    }

}
