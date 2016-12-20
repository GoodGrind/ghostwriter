package io.ghostwriter.openjdk.v7.ast.translator;

import io.ghostwriter.openjdk.v7.model.AstModel;

public interface Translator<T extends AstModel<?>> {

    void translate(T model);

}
