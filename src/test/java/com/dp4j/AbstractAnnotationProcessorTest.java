package com.dp4j;

import com.dp4j.processors.GetInstanceProcessor;
import com.dp4j.processors.InstanceProcessor;
import com.dp4j.processors.SingletonProcessor;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.annotation.processing.Processor;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaCompiler.CompilationTask;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ClassUtils;

/**
 * A base test class for {@link Processor annotation processor} testing that
 * attempts to compile source test cases that can be found on the classpath.
 *
 * Modified from: @see http://code.google.com/p/aphillips/source/browse/commons-test-support/trunk/src/main/java/com/qrmedia/commons/test/annotation/processing/AbstractAnnotationProcessorTest.java
 *
 */
public abstract class AbstractAnnotationProcessorTest {

    private static final String SOURCE_FILE_SUFFIX = ".java";

    /**
     * @return the processor instances that should be tested
     */
    protected abstract Collection<? extends Processor> getProcessors();

    /**
     * Attempts to compile the given compilation units using the Java Compiler
     * API.
     * <p>
     * The compilation units and all their dependencies are expected to be on
     * the classpath.
     *
     * @param compilationUnits
     *            the classes to compile
     * @return the {@link Diagnostic diagnostics} returned by the compilation,
     *         as demonstrated in the documentation for {@link JavaCompiler}
     * @see #compileTestCase(String...)
     */
    protected List<Diagnostic<? extends JavaFileObject>> processTestCase(
            Class<?>... compilationUnits) {
        assert (compilationUnits != null);

        String[] compilationUnitPaths = new String[compilationUnits.length];

        for (int i = 0; i < compilationUnitPaths.length; i++) {
            assert (compilationUnits[i] != null);
            compilationUnitPaths[i] = toResourcePath(compilationUnits[i]);
        }

        return processTestCase(compilationUnitPaths);
    }

    private static String toResourcePath(Class<?> clazz) {
        return ClassUtils.convertClassNameToResourcePath(clazz.getName()) + SOURCE_FILE_SUFFIX;
    }

    /**
     * Attempts to compile the given compilation units using the Java Compiler API.
     * <p>
     * The compilation units and all their dependencies are expected to be on the classpath.
     *
     * @param compilationUnitPaths
     *            the paths of the source files to compile, as would be expected
     *            by {@link ClassLoader#getResource(String)}
     * @return the {@link Diagnostic diagnostics} returned by the compilation,
     *         as demonstrated in the documentation for {@link JavaCompiler}
     * @see #compileTestCase(Class...)
     *
     */
    protected List<Diagnostic<? extends JavaFileObject>> processTestCase(String... compilationUnitPaths) {
        return compileTestCase(true, getProcessors(), compilationUnitPaths);
    }

    protected List<Diagnostic<? extends JavaFileObject>> compileTestCase(String... compilationUnitPaths) {
        return compileTestCase(false, getProcessors(), compilationUnitPaths);
    }

    protected List<Diagnostic<? extends JavaFileObject>> compileTestCase(final Collection<? extends Processor> procs, String... compilationUnitPaths) {
        return compileTestCase(false, procs, compilationUnitPaths);
    }

    protected static List<Diagnostic<? extends JavaFileObject>> compileTestCase(final boolean procOnly, final Collection<? extends Processor> procs, String... compilationUnitPaths) {
        assert (compilationUnitPaths != null);

        Collection<File> compilationUnits;

        try {
            compilationUnits = findClasspathFiles(compilationUnitPaths);
        } catch (IOException exception) {
            throw new IllegalArgumentException(
                    "Unable to resolve compilation units " + Arrays.toString(compilationUnitPaths)
                    + " due to: " + exception.getMessage(),
                    exception);
        }

        final DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<JavaFileObject>();
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnosticCollector, null, null);

        /*
         * Call the compiler with the "-proc:only" option. The "class names"
         * option (which could, in principle, be used instead of compilation
         * units for annotation processing) isn't useful in this case because
         * only annotations on the classes being compiled are accessible.
         *
         * Information about the classes being compiled (such as what they are annotated
         * with) is *not* available via the RoundEnvironment. However, if these classes
         * are annotations, they certainly need to be validated.
         */
        CompilationTask task = compiler.getTask(null, fileManager, diagnosticCollector, procOnly ? Arrays.asList("-proc:only") : null, null,
                fileManager.getJavaFileObjectsFromFiles(compilationUnits));
        task.setProcessors(procs);
        task.call();

        try {
            fileManager.close();
        } catch (IOException exception) {
        }

        return diagnosticCollector.getDiagnostics();
    }

    private static Collection<File> findClasspathFiles(String[] filenames) throws IOException {
        Collection<File> classpathFiles = new ArrayList<File>(filenames.length);

        for (String filename : filenames) {
            classpathFiles.add(new ClassPathResource(getTestFile(filename)).getFile());
        }

        return classpathFiles;
    }

    /**
     * Asserts that the compilation produced no errors, i.e. no diagnostics of
     * type {@link Kind#ERROR}.
     *
     * @param diagnostics
     *            the result of the compilation
     * @see #assertCompilationReturned(Kind, long, List)
     * @see #assertCompilationReturned(Kind[], long[], List)
     */
    protected static void assertCompilationSuccessful(
            List<Diagnostic<? extends JavaFileObject>> diagnostics) {
        assert (diagnostics != null);

        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics) {
            assertFalse("Expected no errors", diagnostic.getKind().equals(Kind.ERROR));
        }

    }

    /**
     * Asserts that the compilation produced results of the following
     * {@link Kind Kinds} at the given line numbers, where the <em>n</em>th kind
     * is expected at the <em>n</em>th line number.
     * <p>
     * Does not check that these is the <em>only</em> diagnostic kinds returned!
     *
     * @param expectedDiagnosticKinds
     *            the kinds of diagnostic expected
     * @param expectedLineNumber
     *            the line numbers at which the diagnostics are expected
     * @param diagnostics
     *            the result of the compilation
     * @see #assertCompilationSuccessful(List)
     * @see #assertCompilationReturned(Kind, long, List)
     */
    protected static void assertCompilationReturned(
            Kind[] expectedDiagnosticKinds, long[] expectedLineNumbers,
            List<Diagnostic<? extends JavaFileObject>> diagnostics) {
        assert ((expectedDiagnosticKinds != null) && (expectedLineNumbers != null)
                && (expectedDiagnosticKinds.length == expectedLineNumbers.length));

        for (int i = 0; i < expectedDiagnosticKinds.length; i++) {
            assertCompilationReturned(expectedDiagnosticKinds[i], expectedLineNumbers[i],
                    diagnostics);
        }

    }

    /**
     * Asserts that the compilation produced a result of the following
     * {@link Kind} at the given line number.
     * <p>
     * Does not check that this is the <em>only</em> diagnostic kind returned!
     *
     * @param expectedDiagnosticKind
     *            the kind of diagnostic expected
     * @param expectedLineNumber
     *            the line number at which the diagnostic is expected
     * @param diagnostics
     *            the result of the compilation
     * @see #assertCompilationSuccessful(List)
     * @see #assertCompilationReturned(Kind[], long[], List)
     */
    protected static void assertCompilationReturned(
            Kind expectedDiagnosticKind, long expectedLineNumber,
            List<Diagnostic<? extends JavaFileObject>> diagnostics) {
        assert ((expectedDiagnosticKind != null) && (diagnostics != null));
        boolean expectedDiagnosticFound = false;

        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics) {

            if (diagnostic.getKind().equals(expectedDiagnosticKind)
                    && (diagnostic.getLineNumber() == expectedLineNumber)) {
                expectedDiagnosticFound = true;
            }

        }

        assertTrue("Expected a result of kind " + expectedDiagnosticKind
                + " at line " + expectedLineNumber, expectedDiagnosticFound);
    }

    protected static void asssertCompilationSuccessful(List<Diagnostic<? extends JavaFileObject>> diagnostics) {
        assert (diagnostics != null);

        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics) {
            assertFalse("Expected no errors", diagnostic.getKind().equals(Kind.ERROR));
        }
    }

    protected static String getTestFile(final String className) {
        return "com/dp4j/samples/" + className + ".java";
    }

    protected Collection<Processor> getProcessors(Processor proc) {
        return Arrays.<Processor>asList(new SingletonProcessor(), new InstanceProcessor(), new GetInstanceProcessor(), proc);
    }
}
