package io.ghostwriter.openjdk.v7.ast.translator;


import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import io.ghostwriter.openjdk.v7.ast.compiler.JavaCompiler;
import io.ghostwriter.openjdk.v7.model.Method;

/*
    If,for-loop (...) statements might contain a single statement in their body, which can trigger tracing (an assignment for example). 
    We assure that these language constructs are always using blocks for their bodies since we might need to add tracing calls.
*/
public class WrapInBlockTranslator extends TreeTranslator implements Translator<Method> {

    final protected JavaCompiler javac;

    public WrapInBlockTranslator(JavaCompiler javac) {
        this.javac = javac;
    }

    @Override
    public void translate(Method model) {
        JCTree.JCMethodDecl representation = model.representation();
        representation.accept(this);
    }

    @Override
    public void visitForLoop(JCTree.JCForLoop forLoop) {
        forLoop.body = wrappedBody(forLoop.body);
        super.visitForLoop(forLoop);
    }

    @Override
    public void visitForeachLoop(JCTree.JCEnhancedForLoop forEachLoop) {
        forEachLoop.body = wrappedBody(forEachLoop.body);
        super.visitForeachLoop(forEachLoop);
    }

    @Override
    public void visitIf(JCTree.JCIf ifStatement) {

        ifStatement.thenpart = wrappedBody(ifStatement.getThenStatement());
        JCTree.JCStatement elseStatement = ifStatement.getElseStatement();
        if (elseStatement != null) {
            ifStatement.elsepart = wrappedBody(elseStatement);
        }
        super.visitIf(ifStatement);
    }

    // return the original body if it is a block, otherwise wrap it in a block construct and return the newly create block
    protected JCTree.JCBlock wrappedBody(JCTree.JCStatement statement) {
        boolean isBlock = statement instanceof JCTree.JCBlock;
        if (isBlock) {
            return (JCTree.JCBlock) statement;
        }

        return javac.block(List.of(statement));
    }

}
