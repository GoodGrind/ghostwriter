package io.ghostwriter.openjdk.v7;

import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import io.ghostwriter.openjdk.v7.ast.collector.Collector;
import io.ghostwriter.openjdk.v7.ast.collector.MethodDeclarationCollector;
import io.ghostwriter.openjdk.v7.ast.compiler.JavaCompiler;
import io.ghostwriter.openjdk.v7.ast.compiler.JavaCompilerHelper;
import io.ghostwriter.openjdk.v7.ast.compiler.Javac;
import io.ghostwriter.openjdk.v7.ast.translator.MethodTranslator;
import io.ghostwriter.openjdk.v7.ast.translator.Translator;
import io.ghostwriter.openjdk.v7.common.Instrumenter;
import io.ghostwriter.openjdk.v7.common.Logger;
import io.ghostwriter.openjdk.v7.model.Method;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author snorbi
 *         Class contains the OpenJDK7 based java compiler specific
 *         implementation details.
 */
public class Javac7Instrumenter implements Instrumenter {

    private Trees trees;
    private JavaCompiler javac;
    private JavaCompilerHelper javacHelper;
    private final Set<String> excludedNames = new HashSet<>();

    @Override
    public void initialize(ProcessingEnvironment processingEnv) {
        setTrees(Trees.instance(processingEnv));
        setJavac(new Javac(processingEnv));
        setJavacHelper(new JavaCompilerHelper(javac));
        initializeExcludedNames(processingEnv);
    }

    protected void initializeExcludedNames(ProcessingEnvironment processingEnv) {
        final String ENV_EXCLUDE_PACKAGES = "GHOSTWRITER_EXCLUDE";
        String excludedNames = processingEnv.getOptions().get(ENV_EXCLUDE_PACKAGES);
        if (excludedNames == null) {
            excludedNames = System.getenv(ENV_EXCLUDE_PACKAGES);
        }
        if (excludedNames != null) {
            for (String excludedName : excludedNames.split("[\\s,]+")) {
                if (excludedName.endsWith(".*")) { // exclusion of a package or of internal classes
                    this.excludedNames.add(excludedName.substring(0,excludedName.length()-2));
                }
                else {
                    this.excludedNames.add(excludedName);
                }
            }
        }
        Logger.note(getClass(),"initializeExcludedNames","GHOSTWRITER_EXCLUDE initialized to "+ this.excludedNames);
    }


    private String getExclusionRule(String qualifiedName) {
        if (excludedNames.isEmpty()) {
            return null;
        }

        while (!excludedNames.contains(qualifiedName)) {
            int lastDot = qualifiedName.lastIndexOf('.');
            if (lastDot <= 0) {
                return  null;
            }
            qualifiedName = qualifiedName.substring(0, lastDot);
        }
        return qualifiedName;
    }

    @Override
    public void process(Element element) {
        if (element instanceof TypeElement) {
            String qualifiedName = ((TypeElement)element).getQualifiedName().toString();
            String exclusionRule = getExclusionRule(qualifiedName);
            if (exclusionRule != null) {
                Logger.note(getClass(), "process", "not instrumenting class '" + qualifiedName + "', '"+exclusionRule+"' is excluded");
                return;
            }
        }

        JCClassDecl klass = toJCClassDecl(element);
        final String className = klass.getSimpleName().toString();
        Logger.note(getClass(), "process", "instrumenting class: " + className);

        // NOTE(snorbi07): IMPORTANT: this only does the processing of TOP LEVEL classes (class source files)!
        // Inner classes are part of the parent classes source tree and processed as such.
        processClass(klass);
    }

    protected JCClassDecl toJCClassDecl(Element element) {
        /*
		 * Cast the JSR269 tree node to its compiler internal type. The
		 * difference between JSR269 tree nodes and internal tree node is, that
		 * JSR269 stops at method level, whereas internally all AST elements are
		 * accessible. We need full access in order to inject entering/leaving
		 * statements.
		 */
        JCTree tree = (JCTree) trees.getTree(element);
        if (!(tree instanceof JCClassDecl)) {
            throw new IllegalArgumentException("Expected type: "
                    + JCClassDecl.class.getSimpleName() + ". Got: "
                    + Element.class.getSimpleName());
        }
        JCClassDecl klass = (JCClassDecl) tree;

        return klass;
    }

    protected void processClass(JCClassDecl klass) {
        Logger.note(getClass(), "processClass", klass.getSimpleName().toString());
        Collector<Method> methodCollector = new MethodDeclarationCollector(javac, klass);
        instrumentMethods(methodCollector.toList());
    }

    protected void instrumentMethods(List<Method> methodModels) {
        Translator<Method> translator = new MethodTranslator(javac, javacHelper);

        for (Method method : methodModels) {
            translator.translate(method);
        }
    }

    protected JavaCompilerHelper getJavacHelper() {
        return javacHelper;
    }

    protected void setJavacHelper(JavaCompilerHelper javacHelper) {
        this.javacHelper = javacHelper;
    }

    protected JavaCompiler getJavac() {
        return javac;
    }

    protected void setJavac(JavaCompiler javac) {
        this.javac = javac;
    }

    protected Trees getTrees() {
        return trees;
    }

    protected void setTrees(Trees trees) {
        this.trees = trees;
    }

}
