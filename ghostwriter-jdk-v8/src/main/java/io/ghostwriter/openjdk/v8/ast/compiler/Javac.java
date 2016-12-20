package io.ghostwriter.openjdk.v8.ast.compiler;


import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;

import javax.annotation.processing.ProcessingEnvironment;

public class Javac extends io.ghostwriter.openjdk.v7.ast.compiler.Javac {

    public Javac(ProcessingEnvironment env) {
        super(env);
    }

    @Override
    public JCTree.JCExpression nullLiteral() {
        JCTree.JCLiteral literal = literal("null");
        literal.typetag = TypeTag.BOT;
        return literal;
    }

    @Override
    public JCTree.JCExpression notEqualExpression(JCTree.JCExpression lhs, JCTree.JCExpression rhs) {
        JCTree.JCBinary binary = make.Binary(JCTree.Tag.NE, lhs, rhs);
        return binary;
    }

    @Override
    public JCTree.JCThrow throwStatement(JCTree.JCExpression expr) {
        // signature of make.Throw changed since version 7. Instead of JCTree it expects a JCExpression
        JCTree.JCThrow throwStatement = make.Throw(expr);
        return throwStatement;
    }

    @Override
    public JCTree.JCPrimitiveTypeTree primitiveType(String type) {
        final JCTree.JCPrimitiveTypeTree result;

        switch (type) {
            case "int":
                result = make.TypeIdent(TypeTag.INT);
                break;
            case "short":
                result = make.TypeIdent(TypeTag.SHORT);
                break;
            case "byte":
                result = make.TypeIdent(TypeTag.BYTE);
                break;
            case "long":
                result = make.TypeIdent(TypeTag.LONG);
                break;
            case "float":
                result = make.TypeIdent(TypeTag.FLOAT);
                break;
            case "double":
                result = make.TypeIdent(TypeTag.DOUBLE);
                break;
            case "boolean":
                result = make.TypeIdent(TypeTag.BOOLEAN);
                break;
            default:
                throw new IllegalArgumentException("Unsupported or unknown primitive type: " + type);
        }

        return result;
    }

    @Override
    public JCTree.JCBinary binary(String operation, JCTree.JCExpression lhs, JCTree.JCExpression rhs) {
        final JCTree.Tag opcode;
        switch (operation) {
            case "<":
                opcode = JCTree.Tag.LT;
                break;
            case "-":
                opcode = JCTree.Tag.MINUS;
                break;
            case "+":
                opcode = JCTree.Tag.PLUS;
                break;
            default:
                throw new IllegalArgumentException("Unsupported binary operator: " + operation);
        }

        return make.Binary(opcode, lhs, rhs);
    }

}
