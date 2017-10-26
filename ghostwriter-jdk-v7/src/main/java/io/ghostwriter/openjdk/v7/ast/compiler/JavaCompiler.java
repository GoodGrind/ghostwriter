package io.ghostwriter.openjdk.v7.ast.compiler;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;

/**
 * All Java compiler API specific complex operations - that do not only deal with reading and writing the properties
 * of the internal types - are collected here.
 */
public interface JavaCompiler {

    /**
     * Returns an expression which evaluates to the default value (Java spec.) of a given type.
     *
     * @param type Type of the default value
     * @return Expression representing a default value
     */
    JCTree.JCExpression defaultValueForType(JCTree.JCExpression type);

    /**
     * Tests whether a javac AST expression resolves to a primitive type.
     *
     * @param type expression to check
     * @return true if the passed expression resolves to a primitive type
     */
    boolean isPrimitiveType(JCExpression type);

    /**
     * Returns the wrapper class representation for a javac finalVariable declaration type expression.
     *
     * @param type primitive type to wrap
     * @return the wrapper representation for a give primitive type
     */
    Class<?> wrapperType(JCPrimitiveTypeTree type);

    /**
     * Returns the fully qualified type of the return parameter.
     * In case of procedures, this will be java.lang.Void.
     *
     * @param method used for introspection
     * @return fully qualified type of the return parameter.
     */
    JCExpression methodReturnType(JCMethodDecl method);

    /**
     * Extract the name of the method based on the javac representation.
     * The symbol is returned as a java.lang.String
     *
     * @param method used for introspection
     * @return name of the method.
     */
    String methodName(JCMethodDecl method);

    /**
     * In javac, dotted access of any kind, from {@code java.lang.String} to
     * {@code var.methodName} is represented by a fold-left of {@code Select}
     * nodes with the leftmost string represented by a {@code Ident} node. This
     * method generates such an expression.
     * 
     * For example, maker.Select(maker.Select(maker.Ident(NAME[java]),
     * NAME[lang]), NAME[String]).
     *
     * @see com.sun.tools.javac.tree.JCTree.JCIdent
     * @see com.sun.tools.javac.tree.JCTree.JCFieldAccess
     *
     * @param expr Java expression to be built into an AST
     * @return AST representation of the passed expression
     */
    JCExpression expression(String expr);

    /**
     * Returns an abstraction for internal compiler strings. They are stored in
     * UTF8 format. Names are stored in a Name.Table, and are unique within
     * that table.
     *
     * @see com.sun.tools.javac.util.Name
     * @see com.sun.tools.javac.util.Name.Table
     *
     * @param identifierName identifier name
     * @return internal representation for an identifier
     */
    Name name(String identifierName);

    /**
     * Construct an array of specified type
     *
     * @param type fully qualified name of the type, such as java.lang.Object
     * @return expression representing the array declaration
     */
    JCNewArray array(JCExpression type);

    /**
     * Return a "constant" value expression for a given value.
     * The type of the value is deduced base on the value itself.
     *
     * In case of null, a NullPointerException will be thrown
     *
     * @param value the value to be represented as a constant
     * @return AST representation of the constant
     *
     */
    JCLiteral literal(Object value);


    /**
     * @param name name of the identifier to be constructed
     * @return compiler representation of the identifier
     */
    JCIdent identifier(String name);

    /**
     * Create a finalVariable declaration
     *
     * @param type  type of the finalVariable
     * @param name  name of the finalVariable
     * @param value right side part of the finalVariable declaration
     * @param parent scope that the variable should belong to
     * @return compiler representation of the finalVariable declaration
     */
    JCVariableDecl finalVariable(JCExpression type, String name, JCExpression value, JCTree parent);


    /**
     * @param identifier left hand side of the assign operation
     * @param expression right hand side of the assign operation
     * @return compiler representation of the assign operation
     */
    JCAssign assign(JCIdent identifier, JCExpression expression);

    /**
     * Wrap expression into a statement type that can be added to method bodies and so on...
     *
     * @param expression expression to wrap
     * @return statement that executes the specified expression
     */
    JCExpressionStatement execute(JCExpression expression);

    /**
     * Create a function invocation expression
     *
     * @param method method to be called
     * @param arguments input arguments for the method
     * @return function call AST
     */
    JCExpressionStatement call(JCExpression method, List<JCExpression> arguments);

    /**
     * Create a function application
     *
     * @param method method application target
     * @param arguments arguments to be applied to a method
     * @return method invocation AST
     *
     */
    JCMethodInvocation apply(JCExpression method, List<JCExpression> arguments);

    /**
     * @param statements list of statements to enclose
     * @return returns the compiler representation of a block containing the given statements
     */
    JCBlock block(List<JCStatement> statements);

    JCExpression declarationType(JCExpression variable);
    
    JCTry tryFinally(JCBlock tryBlock, JCBlock finallyBlock);

    JCCatch catchExpression(JCVariableDecl param, JCBlock body);

    JCThrow throwStatement(JCExpression expr);

    JCTry tryCatchFinally(JCBlock tryBlock, JCCatch catchBlock, JCBlock finallyBlock);

    JCStatement ifCondition(JCExpression condition, JCStatement then);

    JCExpression notEqualExpression(JCExpression lhs, JCExpression rhs);

    JCTree.JCExpression nullLiteral();
    
    boolean isStaticMethod(JCMethodDecl method);

    String fullyQualifiedNameForTypeExpression(JCExpression typeExpression);

    JCAnnotation annotation(String fullyQualifiedName);

    JCVariableDecl catchParameter(String name, JCTree parent);

    JCPrimitiveTypeTree primitiveType(String type);

    JCBinary binary(String operation, JCExpression lhs, JCExpression rhs);

    JCReturn makeReturn(JCExpression result);

    JCExpression castToType(JCTree returnType, JCExpression expression);

    JCArrayAccess arrayAccess(JCExpression indexed, JCExpression index);

    String getOption(String option);
}
