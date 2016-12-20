package io.ghostwriter.openjdk.v7.ast.translator;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import io.ghostwriter.openjdk.v7.ast.collector.ConstructorCallCollector;
import io.ghostwriter.openjdk.v7.ast.compiler.JavaCompilerHelper;
import io.ghostwriter.openjdk.v7.model.Method;

import java.util.Objects;

/**
 * {@code ConstructorTranslator} is used to traverse the instrumented constructor method and assure that the first statement
 * is a call to the original {@code super()/this()} calls in the method. The implementation takes into account that other 
 * instrumentation steps might move the {@code super()/this()} calls into nested blocks, so it searches for
 * the original calls, remove those and moves them to be the first statement in the method body.
 */
public class ConstructorTranslator extends TreeTranslator implements Translator<Method> {

    private JCTree.JCExpressionStatement constructorCall;
    
    private final JavaCompilerHelper helper;

    public ConstructorTranslator(JavaCompilerHelper helper) {
        this.helper = Objects.requireNonNull(helper);
    }

    @Override
    public void translate(Method model) {
        JCTree.JCMethodDecl representation = model.representation();
        if (!helper.isConstructor(representation)) {
            throw new IllegalArgumentException("Provided method is not a constructor: " + representation.toString());
        }
        constructorCall = getConstructorCall(model);

        if (constructorCall != null) {
            representation.accept(this);
        }
    }

    @Override
    public void visitMethodDef(JCTree.JCMethodDecl jcMethodDecl) {
        // since we start the translator from a constructor method, this will be called with the body of the constructor
        super.visitMethodDef(jcMethodDecl);
        jcMethodDecl.body.stats = prependToMethodBody(jcMethodDecl.body, constructorCall);
    }

    private JCTree.JCExpressionStatement getConstructorCall(Method model) {
        JCTree.JCMethodDecl representation = model.representation();
        ConstructorCallCollector constructorCalls = new ConstructorCallCollector(helper, representation);
        java.util.List<JCTree.JCExpressionStatement> jcExpressionStatements = constructorCalls.toList();
        final int EXPECTED_NUMBER_OF_CONSTRUCTOR_CALLS = 1;
        int numCalls = jcExpressionStatements.size();

        JCTree.JCExpressionStatement result = null;
        if (numCalls == EXPECTED_NUMBER_OF_CONSTRUCTOR_CALLS) {
            result = jcExpressionStatements.get(0);
        }
        
        if (numCalls > EXPECTED_NUMBER_OF_CONSTRUCTOR_CALLS) {
            throw new IllegalStateException("Method contains '" + numCalls + "' constructor calls! Method: " + representation);
        }

        return result;
    }

    private List<JCTree.JCStatement> prependToMethodBody(JCTree.JCBlock body, JCTree.JCExpressionStatement superOrThisCall) {
        List<JCTree.JCStatement> statements = body.stats;
        return statements.prepend(superOrThisCall);
    }
    
    @Override
    public void visitBlock(JCTree.JCBlock block) {
        List<JCTree.JCStatement> statements = removeConstructorCallsFromBlock(block);
        super.visitBlock(block);
        block.stats = statements;
        result = block;
    }

    private List<JCTree.JCStatement> removeConstructorCallsFromBlock(JCTree.JCBlock block) {
        ListBuffer<JCTree.JCStatement> sanitizedStatements = new ListBuffer<>();
        
        // remove/delete operations are not supported by statement iterators so we create a copy without the super/this call
        for (JCTree.JCStatement statement : block.getStatements()) {
            if (!helper.isStatementSuperOrThisCall(statement)) {  // do not include the super/this call, that will be moved as the first statement in the method body
                sanitizedStatements.add(statement);
            }
        }
        
        return sanitizedStatements.toList();
    }   
    
}
