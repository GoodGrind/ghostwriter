package io.ghostwriter.openjdk.v7.model;


import com.sun.tools.javac.tree.JCTree;

import java.util.Objects;

//NOTE (snorbi07): yes... I know, leaking implementation details. This is why it is in an implementation specific package.
// used for providing the possibility to pass around context if necessary to avoid "global" state...
public class AstModel<T extends JCTree> {

    private final T representation;

    public AstModel(T representation) {
        this.representation = Objects.requireNonNull(representation, "Must provide a valid AST representation!");
    }

    public T representation() {
        return representation;
    }

}
