package io.ghostwriter.openjdk.v7.ast.compiler;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import io.ghostwriter.openjdk.v7.common.Logger;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeKind;
import java.util.Map;

public class Javac implements JavaCompiler {

    protected final TreeMaker make;
    protected final JavacElements elements;
    protected final Context context;
    private final Map<String, String> options;

    public Javac(ProcessingEnvironment env) {
        if (env == null) {
            throw new IllegalArgumentException("Must provide a "
                    + ProcessingEnvironment.class.getSimpleName()
                    + " instance!");
        }

        options = env.getOptions();
        JavacProcessingEnvironment javacEnv = (JavacProcessingEnvironment) env;

        context = javacEnv.getContext();
        make = TreeMaker.instance(context);
        elements = JavacElements.instance(context);
    }

    @Override
    public String fullyQualifiedNameForTypeExpression(JCExpression typeExpression) {
        if (typeExpression == null) {
            return Void.class.getName();
        }

        if (isPrimitiveType(typeExpression)) {
            JCPrimitiveTypeTree primitiveType = (JCPrimitiveTypeTree) typeExpression;
            Class<?> wrapper = wrapperType(primitiveType);
            return wrapper.getName();
        }

        Type type = typeExpression.type;

        final boolean isAnonymousClass = type == null;
        if (isAnonymousClass) {
            return typeExpression.toString();
        }

        final boolean isArrayType = type instanceof Type.ArrayType;
        if (isArrayType) {
            return type.getModelType().toString();
        }

        // does not include type parameters (generics)
        // to get the generic information as well, do:
        // String fullyQualifiedNameWithGenerics = type.toString();
        return type.tsym.toString();
    }

    @Override
    public JCExpression declarationType(JCExpression typeExpression) {
        JCExpression declType = typeExpression;
        if (typeExpression == null) {
            declType = expression(Void.class.getName());
        }

        return declType;
    }

    @Override
    public boolean isPrimitiveType(JCExpression type) {
        return type instanceof JCPrimitiveTypeTree;
    }

    @Override
    public Class<?> wrapperType(JCPrimitiveTypeTree type) {
        Class<?> wrapper = null;

        final TypeKind primitiveTypeKind = type.getPrimitiveTypeKind();

        switch (primitiveTypeKind) {
            case BYTE:
                wrapper = Byte.class;
                break;

            case CHAR:
                wrapper = Character.class;
                break;

            case SHORT:
                wrapper = Short.class;
                break;

            case INT:
                wrapper = Integer.class;
                break;

            case LONG:
                wrapper = Long.class;
                break;

            case FLOAT:
                wrapper = Float.class;
                break;

            case DOUBLE:
                wrapper = Double.class;
                break;

            case BOOLEAN:
                wrapper = Boolean.class;
                break;

            case VOID:
                wrapper = Void.class;
                break;

            default:
                throw new IllegalArgumentException("Unsupported primitive type: " + type);
        }

        assert wrapper != null;
        return wrapper;
    }

    @Override
    public JCExpression methodReturnType(JCMethodDecl method) {
        JCExpression resultTypeExpression = method.restype;
        JCExpression resultType = declarationType(resultTypeExpression);
        Logger.note(getClass(), "methodReturnType", resultType.toString());

        return resultType;
    }

    @Override
    public String methodName(JCMethodDecl method) {
        Name name = method.getName();
        String methodName = "";
        if (name != null) {
            methodName = name.toString();
        } else {
            Logger.error(getClass(), "methodName",
                    "method does not have a valid name!");
        }
        return methodName;
    }

    @Override
    public JCExpression expression(String expr) {
        String[] elements = expr.split("\\.");
        return expression(elements);
    }

    protected JCExpression expression(String... elements) {
        if (elements.length < 1) {
            throw new IllegalArgumentException(
                    "The expression needs to contain at least one element!");
        }
        // NOTE: we might check whether the given string expression contains a
        // valid Java code snippet.
        // Not a blocking or mandatory task, since the compilation will fail
        // anyways with a syntax error.

        // Create the initial part of the expression, which has a type of
        // JCIdent.
        // This is the simplest expression form.
        final int INITIAL_PART_OFFSET = 0;
        String initialPart = elements[INITIAL_PART_OFFSET];
        JCExpression initialExpression = make.Ident(name(initialPart));
        JCExpression expression = initialExpression;

        // If required, create the JCSelect parts of the expression
        int numberOfFieldAccessParts = elements.length;
        final int FIELD_ACCESS_PART_OFFSET = 1;
        for (int index = FIELD_ACCESS_PART_OFFSET; index < numberOfFieldAccessParts; ++index) {
            String currentPart = elements[index];
            expression = make.Select(expression, name(currentPart));
        }

        return expression;
    }

    @Override
    public Name name(String identifierName) {
        return elements.getName(identifierName);
    }

    @Override
    public JCNewArray array(JCExpression type) {
        if (type == null) {
            throw new IllegalArgumentException("Must provide a valid, non 'null' instance of " + JCExpression.class.getSimpleName());
        }

        // Example of [0]: List.<JCExpression>of(make.Literal(getCompileTimeConstant(TypeTags.class, "INT"), 0))
        List<JCExpression> dimensions = List.<JCExpression>nil();
        List<JCExpression> values = null; // FIXME: should'nt this be List.<JCExpression> nil()?
        JCExpression typeExpression = type;

        return makeArray(typeExpression, dimensions, values);
    }

    /**
     * Constructs and array expression with the specified dimensions and initial values.
     *
     * @param type       Type of the constructed array
     * @param dimensions dimensions Dimensions of the array
     * @param values     Initial values of the array
     * @return Array expression
     */
    protected JCNewArray makeArray(JCExpression type, List<JCExpression> dimensions, List<JCExpression> values) {
        return make.NewArray(type, dimensions, values);
    }

    @Override
    public JCLiteral literal(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Provided 'value' parameter is null!");
        }

        return make.Literal(value);
    }

    @Override
    public JCIdent identifier(String name) {
        Name identifierName = name(name);
        return make.Ident(identifierName);
    }

    @Override
    public JCExpressionStatement call(JCExpression method, List<JCExpression> arguments) {
        final JCMethodInvocation apply = apply(method, arguments);
        JCExpressionStatement execute = make.Exec(apply);

        return execute;
    }

    @Override
    public JCMethodInvocation apply(JCExpression method, List<JCExpression> arguments) {
        List<JCExpression> typeArguments = List.nil();

        return make.Apply(typeArguments, method, arguments);
    }

    @Override
    public JCVariableDecl finalVariable(JCExpression type, String name, JCExpression value, JCTree parent) {
        Name variableName = name(name);
        JCModifiers modifiers = make.Modifiers(Flags.FINAL);
        JCVariableDecl varDef = make.VarDef(modifiers, variableName, type, value);
        varDef.pos = parent.pos;
        return varDef;
    }

    @Override
    public JCVariableDecl catchParameter(String name, JCTree parent) {
        final JCExpression type = expression("java.lang.Throwable");
        final long flags = Flags.PARAMETER | Flags.FINAL;
        JCModifiers modifiers = make.Modifiers(flags);
        Name variableName = name(name);
        final JCVariableDecl jcVariableDecl = make.VarDef(modifiers, variableName, type, null);
        // See https://gitlab.com/goodgrind/GhostWriter/issues/63
        // Setting the position of the catch parameter based on the method scope is necessary to avoid the Flow analysis error
        // This fix was inspired by the implementation of Lombok's SneakyThrows... in other words I have no idea what I'm doing.
        jcVariableDecl.pos = parent.pos;
        return jcVariableDecl;
    }

    @Override
    public JCAssign assign(JCIdent identifier, JCExpression expression) {
        JCAssign assignExpression = make.Assign(identifier, expression);
        return assignExpression;
    }

    @Override
    public JCExpressionStatement execute(JCExpression expression) {
        JCExpressionStatement exec = make.Exec(expression);
        return exec;
    }

    @Override
    public JCThrow throwStatement(JCExpression expr) {
        JCThrow throwStatement = make.Throw(expr);
        return throwStatement;
    }

    @Override
    public JCStatement ifCondition(JCExpression condition, JCStatement then) {
        JCIf ifStatement = make.If(condition, then, null);
        return ifStatement;
    }

    @Override
    public JCBlock block(List<JCStatement> statements) {
        return make.Block(0L, statements);
    }

    @Override
    public JCTry tryFinally(JCBlock tryBlock, JCBlock finallyBlock) {
        JCTry tryFinally = make.Try(tryBlock, List.<JCCatch>nil(), finallyBlock);
        return tryFinally;
    }

    @Override
    public JCTry tryCatchFinally(JCBlock tryBlock, JCCatch catchBlock, JCBlock finallyBlock) {
        JCTry tryFinally = make.Try(tryBlock, List.of(catchBlock), finallyBlock);
        return tryFinally;
    }

    @Override
    public JCCatch catchExpression(JCVariableDecl param, JCBlock body) {
        JCCatch catchExpr = make.Catch(param, body);
        return catchExpr;
    }

    @Override
    public JCExpression notEqualExpression(JCExpression lhs, JCExpression rhs) {
        JCBinary binary = make.Binary(JCTree.NE, lhs, rhs);
        return binary;
    }

    @Override
    public JCTree.JCExpression defaultValueForType(JCTree.JCExpression type) {
        JCTree.JCExpression defaultValue = nullLiteral();
        if (!isPrimitiveType(type)) {
            return defaultValue;
        }

        JCTree.JCPrimitiveTypeTree primitiveType = (JCTree.JCPrimitiveTypeTree) type;
        final TypeKind primitiveTypeKind = primitiveType.getPrimitiveTypeKind();

        switch (primitiveTypeKind) {
            case BYTE:
            case CHAR:
            case SHORT:
            case INT:
                defaultValue = literal(0);
                break;
            case LONG:
                defaultValue = literal(0L);
                break;
            case FLOAT:
                defaultValue = literal(0.0F);
                break;
            case DOUBLE:
                defaultValue = literal(0.0);
                break;
            case BOOLEAN:
                defaultValue = literal(Boolean.FALSE);
                break;
            case VOID:
                Logger.error(getClass(), "defaultValueForType", "type 'void' does not have a default value!");
                break;
            default:
                Logger.error(getClass(), "defaultValueForType", "unsupported primitive type: " + primitiveType);
                break;
        }

        return defaultValue;
    }

    @Override
    public JCTree.JCExpression nullLiteral() {
        JCTree.JCLiteral literal = literal("null");
        literal.typetag = TypeTags.BOT;
        return literal;
    }

    @Override
    public boolean isStaticMethod(JCMethodDecl method) {
        JCTree.JCModifiers modifiers = method.mods;
        return (modifiers.flags & Flags.STATIC) != 0L;
    }

    @Override
    public JCAnnotation annotation(String fullyQualifiedName) {
        JCExpression type = expression(fullyQualifiedName);
        return make.Annotation(type, List.<JCExpression>nil());
    }

    @Override
    public JCPrimitiveTypeTree primitiveType(String type) {
        final int typeKind;
        switch (type) {
            case "byte":
                typeKind = TypeKind.BYTE.ordinal();
                break;
            case "char":
                typeKind = TypeKind.CHAR.ordinal();
                break;
            case "short":
                typeKind = TypeKind.SHORT.ordinal();
                break;
            case "int":
                typeKind = TypeKind.INT.ordinal();
                break;
            case "long":
                typeKind = TypeKind.LONG.ordinal();
                break;
            case "float":
                typeKind = TypeKind.FLOAT.ordinal();
                break;
            case "double":
                typeKind = TypeKind.DOUBLE.ordinal();
                break;
            case "boolean":
                typeKind = TypeKind.BOOLEAN.ordinal();
                break;
            default:
                throw new IllegalArgumentException("Unsupported or unknown primitive type: " + type);
        }

        // we need to add 1 to the ordinal value, since TypeKind values start from 0, while the int codes start from 1
        // check JCPrimitiveTypeTree.getPrimitiveTypeKind for details
        return make.TypeIdent(typeKind + 1);
    }

    @Override
    public JCBinary binary(String operation, JCExpression lhs, JCExpression rhs) {
        final int opcode;
        switch (operation) {
            case "<":
                opcode = JCTree.LT;
                break;
            case "-":
                opcode = JCTree.MINUS;
                break;
            case "+":
                opcode = JCTree.PLUS;
                break;
            default:
                throw new IllegalArgumentException("Unsupported binary operator: " + operation);
        }

        return make.Binary(opcode, lhs, rhs);
    }

    @Override
    public JCReturn makeReturn(JCExpression result) {
        return make.Return(result);
    }

    @Override
    public JCExpression castToType(JCTree returnType, JCExpression expression) {
        return make.TypeCast(returnType, expression);
    }

    @Override
    public JCArrayAccess arrayAccess(JCExpression indexed, JCExpression index) {
        return make.Indexed(indexed, index);
    }

    @Override
    public String getOption(String option) {
        String ret = options.get(option);
        if (ret == null) {
            ret = System.getenv(option);
        }
        return ret;
    }
}
