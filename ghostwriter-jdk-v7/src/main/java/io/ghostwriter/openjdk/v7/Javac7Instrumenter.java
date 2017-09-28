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
import java.util.Arrays;
import java.util.Collections;
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
    private boolean isAnnotatedOnlyMode;
    private final Set<String> excludedClasses = new HashSet<>();
    private final Set<String> excludedMethodNames = new HashSet<>();
    private static final List<String> DEFAULT_EXCLUDED_METHODS = Collections.unmodifiableList(Arrays.asList("toString", "equals", "hashCode", "compareTo"));

    @Override
    public void initialize(ProcessingEnvironment processingEnv) {
        setTrees(Trees.instance(processingEnv));
        setJavac(new Javac(processingEnv));
        setJavacHelper(new JavaCompilerHelper(javac));
        initializeFromEnv(processingEnv);
    }

    protected void initializeFromEnv(ProcessingEnvironment processingEnv) {
        initializeExcludedClasses(processingEnv);
        initializeExcludedMethodNames(processingEnv);
        initializeAnnotationOnlyMode(processingEnv);
    }

    protected final void initializeExcludedClasses(ProcessingEnvironment processingEnv) {
        final String envExcludes = "GHOSTWRITER_EXCLUDE";
        String rawExcludedNames = processingEnv.getOptions().get(envExcludes);
        if (rawExcludedNames == null) {
            rawExcludedNames = System.getenv(envExcludes);
        }
        if (rawExcludedNames != null) {
            for (String excludedName : rawExcludedNames.split("[\\s,]+")) {
                if (excludedName.endsWith(".*")) { // exclusion of a package or of internal classes
                    excludedClasses.add(excludedName.substring(0,excludedName.length()-2));
                }
                else {
                    excludedClasses.add(excludedName);
                }
            }
        }
        Logger.note(getClass(),"initializeExcludedNames","GHOSTWRITER_EXCLUDE initialized to "+ this.excludedClasses);
    }

    protected final void initializeExcludedMethodNames(ProcessingEnvironment processingEnv) {
        final String envExcludeMethods = "GHOSTWRITER_EXCLUDE_METHODS";
        String rawExcludedMethodNames = processingEnv.getOptions().get(envExcludeMethods);
        if (rawExcludedMethodNames == null) {
            rawExcludedMethodNames = System.getenv(envExcludeMethods);
        }
        if (rawExcludedMethodNames != null) {
            String[] names = rawExcludedMethodNames.split("[\\s,]+");
            excludedMethodNames.addAll(Arrays.asList(names));
            Logger.note(getClass(), "initializeExcludedMethodNames", "custom exclude methods: " + excludedMethodNames);
        }
        else {
            excludedMethodNames.addAll(DEFAULT_EXCLUDED_METHODS);
            Logger.note(getClass(), "initializeExcludedMethodNames", "default exclude methods: " + excludedMethodNames);
        }
    }

    protected final void initializeAnnotationOnlyMode(ProcessingEnvironment processingEnv) {
        final String envAnnotatedOnly = "GHOSTWRITER_ANNOTATED_ONLY";
        String rawEnvAnnotatedOnly = processingEnv.getOptions().get(envAnnotatedOnly);
        if (rawEnvAnnotatedOnly == null) {
            rawEnvAnnotatedOnly = System.getenv(envAnnotatedOnly);
        }
        isAnnotatedOnlyMode = rawEnvAnnotatedOnly != null && Boolean.parseBoolean(rawEnvAnnotatedOnly);
        Logger.note(getClass(), "initializeAnnotationOnlyMode", "annotated only mode enabled: " + isAnnotatedOnlyMode);
    }

    private String getExclusionRule(String qualifiedName) {
        if (excludedClasses.isEmpty()) {
            return null;
        }

        while (!excludedClasses.contains(qualifiedName)) {
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
        Translator<Method> translator = getMethodTranslator();

        for (Method method : methodModels) {
            if (!isMethodExcluded(method)) {
                translator.translate(method);
            }
        }
    }

    protected final boolean isMethodExcluded(Method method) {
        if (isMethodExcludedByEnv(method)) {
            Logger.note(getClass(), "isMethodExcluded", "skipping instrumentation of method (env): " + method.getName());
            return true;
        }
        if (isMethodExcludedByAnnotation(method)) {
            Logger.note(getClass(), "isMethodExcluded", "skipping instrumentation of method (annotation): " + method.getName());
            return true;
        }
        if (isAnnotatedOnlyMode() && !isIncludedClassOrMethod(method)) {
            Logger.note(getClass(), "isMethodExcluded", "skipping un-annotated method: " + method.getName());
            return true;
        }
        return false;
    }

    protected final boolean isMethodExcludedByEnv(Method model) {
        return excludedMethodNames.contains(model.getName());
    }

    protected final boolean isMethodExcludedByAnnotation(Method model) {
        final JCTree.JCMethodDecl methodRepresentation = model.representation();
        final boolean isMethodExcluded = javacHelper.isExcluded(methodRepresentation);
        final JCTree.JCClassDecl classRepresentation = model.getClazz().representation();
        final boolean isClassExcluded = javacHelper.isExcluded(classRepresentation);

        // a method should be skipped if the class it belongs to has an Exclude annotation
        // or the method itself was annotated with Exclude.
        return isClassExcluded || isMethodExcluded;
    }

    protected final boolean isIncludedClassOrMethod(Method model) {
        final JCTree.JCMethodDecl method = model.representation();
        final JCTree.JCClassDecl clazz = model.getClazz().representation();
        return javacHelper.isIncluded(clazz) || javacHelper.isIncluded(method);
    }

    protected final boolean isAnnotatedOnlyMode() {
        return isAnnotatedOnlyMode;
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

    protected Translator<Method> getMethodTranslator() {
        return new MethodTranslator(javac, javacHelper);
    }
}
