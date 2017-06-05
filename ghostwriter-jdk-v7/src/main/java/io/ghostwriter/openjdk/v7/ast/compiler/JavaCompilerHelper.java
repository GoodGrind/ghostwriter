package io.ghostwriter.openjdk.v7.ast.compiler;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import io.ghostwriter.openjdk.v7.model.Method;

import java.util.Objects;


/**
 * Contains helper methods that are used to deduce information about existing code and code model or
 * create Java language representations of various elements. 
 * Helper methods built on the {@code JavaCompiler} interface are collected in this file.
 */
public class JavaCompilerHelper {

    private static final String EXCLUDE_ANNOTATION_TYPE = "io.ghostwriter.annotation.Exclude";

    private static final String TIMEOUT_ANNOTATION_TYPE = "io.ghostwriter.annotation.Timeout";

    private static final String INCLUDE_ANNOTATION_TYPE = "io.ghostwriter.annotation.Include";

    private final JavaCompiler javac;

    public JavaCompilerHelper(JavaCompiler javac) {
        this.javac = Objects.requireNonNull(javac);
    }

    public JCTree.JCLiteral methodName(Method model) {
        String name = model.getName();
        return javac.literal(name);
    }

    public JCTree.JCExpression methodContext(Method model) {
        JCTree.JCExpression context = javac.identifier("this");
        boolean isStaticMethod = javac.isStaticMethod(model.representation());
        if (isStaticMethod) {
            String fullyQualifiedClassName = model.getClazz().getFullyQualifiedClassName();
            String staticClassContext = fullyQualifiedClassName + ".class";
            context  = javac.expression(staticClassContext);
        }

        return context;
    }

    public boolean isConstructor(JCTree.JCMethodDecl methodDecl) {
        final String CONSTRUCTOR_IDENTIFIER = "<init>";
        final String methodName = javac.methodName(methodDecl);
        return CONSTRUCTOR_IDENTIFIER.equals(methodName);
    }

    public boolean hasResult(JCTree.JCMethodDecl methodDecl) {
        JCTree.JCExpression type = methodDecl.restype;
        boolean result = false;

        if (type != null) {
            String typeName = type.toString();
            result = !"void".equals(typeName) && !"java.lang.Void".equals(typeName);
        }

        return result;
    }

    public boolean isStatementSuperOrThisCall(JCTree.JCStatement statement) {
        final String THIS = "this(";
        final String SUPER = "super(";
        String expressionStatement = statement.toString();
        boolean result = false;
        boolean isThisCall = expressionStatement.startsWith(THIS);
        boolean isSuperCall = expressionStatement.startsWith(SUPER);
        if (isThisCall || isSuperCall) {
            result = true;
        }

        return result;
    }

    private static JCTree.JCAnnotation getExcludeAnnotation(JCTree.JCClassDecl classDecl) {
        List<JCTree.JCAnnotation> annotations = classDecl.mods.annotations;

        return findFirstAnnotation(annotations, EXCLUDE_ANNOTATION_TYPE);
    }

    private static JCTree.JCAnnotation getIncludeAnnotation(JCTree.JCClassDecl classDecl) {
        List<JCTree.JCAnnotation> annotations = classDecl.mods.annotations;

        return findFirstAnnotation(annotations, INCLUDE_ANNOTATION_TYPE);
    }

    public boolean isExcluded(JCTree.JCClassDecl classDecl) {
        return getExcludeAnnotation(classDecl) != null;
    }

    public boolean isIncluded(JCTree.JCClassDecl classDecl) {
        return getIncludeAnnotation(classDecl) != null;
    }

    private static JCTree.JCAnnotation getExcludeAnnotation(JCTree.JCMethodDecl methodDecl) {
        List<JCTree.JCAnnotation> annotations = methodDecl.mods.annotations;

        return findFirstAnnotation(annotations, EXCLUDE_ANNOTATION_TYPE);
    }

    private static JCTree.JCAnnotation getIncludeAnnotation(JCTree.JCMethodDecl methodDecl) {
        List<JCTree.JCAnnotation> annotations = methodDecl.mods.annotations;

        return findFirstAnnotation(annotations, INCLUDE_ANNOTATION_TYPE);
    }

    public boolean isExcluded(JCTree.JCMethodDecl methodDecl) {
        return getExcludeAnnotation(methodDecl) != null;
    }

    public boolean isIncluded(JCTree.JCMethodDecl methodDecl) {
        return getIncludeAnnotation(methodDecl) != null;
    }

    private static JCTree.JCAnnotation getExcludeAnnotation(JCTree.JCVariableDecl variableDecl) {
        final List<JCTree.JCAnnotation> annotations = variableDecl.mods.annotations;
        return findFirstAnnotation(annotations, EXCLUDE_ANNOTATION_TYPE);
    }

    public boolean isExcluded(JCTree.JCVariableDecl variableDecl) {
        return getExcludeAnnotation(variableDecl) != null;
    }

    public JCTree.JCAnnotation getTimeoutAnnotation(JCTree.JCMethodDecl methodDecl) {
        final List<JCTree.JCAnnotation> annotations = methodDecl.mods.annotations;
        return findFirstAnnotation(annotations, TIMEOUT_ANNOTATION_TYPE);
    }

    private static JCTree.JCAnnotation findFirstAnnotation(List<JCTree.JCAnnotation> annotations, String targetAnnotationType) {
        if (targetAnnotationType != null && targetAnnotationType.isEmpty()) {
            throw new IllegalArgumentException("Specified annotation type is not valid: " + targetAnnotationType);
        }

        JCTree.JCAnnotation resultAnnotation = null;
        for (JCTree.JCAnnotation annotation : annotations) {
            // NOTE(snorbi07): annotated local variables do not have a type specified for whatever reason...
            // Example: @io.ghostwriter.annotation.Exclude int someVariable = 3;
            boolean isAnnotatedLocalVariable = annotation.type == null;
            String actualAnnotationType;
            String expectedAnnotationType;

            if (!isAnnotatedLocalVariable) {
                final String annotationFullyQualifiedName = annotation.type.toString();
                actualAnnotationType = annotationFullyQualifiedName;
                expectedAnnotationType = targetAnnotationType;
            }
            else {
                final String annotationType = annotation.getAnnotationType().toString();
                actualAnnotationType = simpleNameOfAnnotation(annotationType);
                expectedAnnotationType = simpleNameOfAnnotation(targetAnnotationType);
            }

            if (actualAnnotationType.equals(expectedAnnotationType)) {
                resultAnnotation = annotation;
                break;
            }
        }

        return resultAnnotation;
    }

    private static String simpleNameOfAnnotation(String fullyQualifiedTypeOfAnnotation) {
        String[] tokens = fullyQualifiedTypeOfAnnotation.split("[.]");

        String result = fullyQualifiedTypeOfAnnotation;
        if (tokens.length > 0) {
            result = tokens[tokens.length - 1]; // last token should be the type's simple name
        }

        return result;
    }

}
