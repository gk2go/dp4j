package com.dp4j;

import java.util.Collection;
import java.util.List;
import javax.annotation.processing.Processor;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 *
 * @author simpatico
 */
public abstract class AbstractAccessibleTest {

    protected abstract void dummy();

    protected List<Diagnostic<? extends JavaFileObject>> compileTestCase(String... compilationUnitPaths) {
        return compileTestCase(false, null, compilationUnitPaths);
    }

    protected static List<Diagnostic<? extends JavaFileObject>> compileTestCase(final boolean procOnly, final Collection<? extends Processor> procs, String... compilationUnitPaths) {
        return null;
    }
}
