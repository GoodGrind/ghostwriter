package io.ghostwriter.openjdk.v7.model;


import com.sun.tools.javac.tree.JCTree.JCMethodDecl;

import java.util.List;
import java.util.Objects;

public class Method extends AstModel<JCMethodDecl> {    
    private final String name;
    private final Clazz clazz;
    private final List<Parameter> parameters;

    public Method(String name, Clazz clazz, List<Parameter> parameters, JCMethodDecl representation) {
        super(representation);
        Objects.requireNonNull(clazz, "Must provide a valid class AST model!");
        Objects.requireNonNull(name, "Must provide a valid parameter name!");
        if ("".equals(name)) {
            throw new IllegalArgumentException("Must provide a non-empty method name!");
        }

        this.name = name;
        this.clazz = clazz;
        this.parameters = parameters;
    }

    public Clazz getClazz() {
        return clazz;
    }
    
    public String getName() {
        return name;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        return "Method [class=" + clazz.getFullyQualifiedClassName() + ", name=" + name + ", parameters=" + parameters + "]";
    }

}
