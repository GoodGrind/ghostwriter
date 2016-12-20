package io.ghostwriter.openjdk.v7.ast.collector;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import io.ghostwriter.openjdk.v7.common.Logger;

import java.util.*;

public class Collector<T> extends TreeTranslator implements Iterable<T> {

    final private List<T> items = new ArrayList<>();

    final private JCTree rootElement;
    
    // the Collector is lazy because of initialization requirements
    private boolean hasExecuted = false;
    
    public Collector(JCTree rootElement) {
        super();
        this.rootElement = Objects.requireNonNull(rootElement);
    }
    
    protected Collector<T> execute() {
        rootElement.accept(this);
        return this;
    }

    protected void collect(T item) {
        if (item == null) {
            Logger.warning(getClass(), "collect", "tried to collect null item! Skipping!");
            return;
        }

        assert items != null;
        items.add(item);
    }
    
    protected void collectAll(List<T> items) {
        for (T item : items) {
            collect(item);
        }
    }

    @Override
    public Iterator<T> iterator() {
        // toList() assures that the collecting was executed.
        return toList().iterator();
    }

    public List<T> toList() {
        if (!hasExecuted) {
            execute();
        }
        
        return Collections.unmodifiableList(items);
    }
}
