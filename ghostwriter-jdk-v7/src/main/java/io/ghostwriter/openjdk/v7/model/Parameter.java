package io.ghostwriter.openjdk.v7.model;


import com.sun.tools.javac.tree.JCTree.JCVariableDecl;

import java.util.Objects;

public class Parameter extends AstModel<JCVariableDecl> {
    private final String name;
    // fully qualified name of the type. Representing
    // it with Class<?> is not possible, since most
    // of the classes are only available at run-time
    private final String type;

    public Parameter(String name, String type, JCVariableDecl representation) {
        super(representation);
        Objects.requireNonNull(name, "Must provide a valid parameter name!");
        Objects.requireNonNull(type, "Must provide a valid parameter type!");
        if ("".equals(name)) {
            throw new IllegalArgumentException("Must provide a non-empty parameter name!");
        }
        if ("".equals(type)) {
            throw new IllegalArgumentException("Must provide a non-empty, fully qualified name of the parameter type!");
        }

        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns the fully qualified name of the parameter type
     *
     * @return fully qualified name of the parameter type
     */
    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Parameter [name=" + name + ", type=" + type + "]";
    }

}
