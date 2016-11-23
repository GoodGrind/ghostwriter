package io.ghostwriter.openjdk.v7.model;


import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Name;

public class Clazz extends AstModel<JCTree.JCClassDecl> {

    public Clazz(JCTree.JCClassDecl representation) {
        super(representation);
    }
    
    public String getFullyQualifiedClassName() {
        JCTree.JCClassDecl classDecl = representation();
        final Symbol.ClassSymbol sym = classDecl.sym;
        final boolean isAnonymousClass = sym == null;
        String name = "";
        if (!isAnonymousClass) {
            final Name qualifiedName = sym.getQualifiedName();
            name = qualifiedName.toString();
        }
        return name;
    }

}
