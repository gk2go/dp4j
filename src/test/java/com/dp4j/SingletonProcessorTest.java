/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j;

import javax.tools.Diagnostic.Kind;
import com.dp4j.processors.*;
import java.util.Arrays;
import com.qrmedia.commons.test.annotation.processing.*;
import java.util.Collection;
import java.util.List;
import javax.annotation.processing.Processor;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author simpatico
 */
public class SingletonProcessorTest extends AbstractAnnotationProcessorTest {

    @Override
    protected Collection<Processor> getProcessors() {
        return Arrays.<Processor>asList(new SingletonProcessor());
    }

    final String getTestFile(final String className){
        return "com/dp4j/samples/"+ className + ".java";
    }
    @Test
    public void testAllPrivateConstructors() {
//        asssertCompilationSuccessful(compileTestCase(MySingleton.class));
        assertCompilationReturned(Kind.ERROR, -1, compileTestCase(getTestFile("SingletonWithPublicConstructor")));
        //FIXME: -1? How to figure out
        assertCompilationReturned(Kind.ERROR, -1, compileTestCase(getTestFile("ExtendingSingleton")));
        assertCompilationReturned(Kind.ERROR, -1, compileTestCase(getTestFile("SingletonWithOnlyOneConstructor")));
    }

    private void asssertCompilationSuccessful(List<Diagnostic<? extends JavaFileObject>> diagnostics) {
        assert (diagnostics != null);

        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics) {
            assertFalse("Expected no errors", diagnostic.getKind().equals(Kind.ERROR));
        }
    }
}