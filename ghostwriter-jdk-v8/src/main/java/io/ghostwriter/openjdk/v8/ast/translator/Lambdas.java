package io.ghostwriter.openjdk.v8.ast.translator;


import com.sun.tools.javac.tree.JCTree;
import io.ghostwriter.openjdk.v7.model.Method;

import java.util.stream.Collectors;

class Lambdas {

    private Lambdas() {
        throw new UnsupportedOperationException("class is not designed for instantiation");
    }

    static public String nameFor(Method model, JCTree.JCLambda visitedLambda) {
        // we want to return: someMethod: (param1, param2)->;
        // NOTE(snorbi07): potentially we could return the method name defined in the used @Functional interface.

        final String enclosingMethodName = model.getName();
        final String parameterList = visitedLambda.getParameters().stream()
                .map(var -> var.getName().toString())
                .collect(Collectors.joining(", "));

        final StringBuilder sb = new StringBuilder();
        sb.append(enclosingMethodName).append(": (").append(parameterList).append(")").append("->");

        return sb.toString();
    }

}
