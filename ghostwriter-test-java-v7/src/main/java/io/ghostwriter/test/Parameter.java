package io.ghostwriter.test;

final public class Parameter<T> {

    private final String name;

    private final T value;

    public Parameter(String name, T value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public T getValue() {
        return value;
    }
}
