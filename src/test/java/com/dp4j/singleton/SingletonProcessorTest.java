/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.singleton;

import com.dp4j.AbstractAnnotationProcessorTest;
import com.dp4j.processors.GetInstanceProcessor;
import com.dp4j.processors.InstanceProcessor;
import com.dp4j.processors.SingletonProcessor;
import java.util.Arrays;
import java.util.Collection;
import javax.annotation.processing.Processor;
import org.junit.Test;


import javax.tools.Diagnostic.Kind;

/**
 *
 * @author simpatico
 */
public class SingletonProcessorTest extends AbstractAnnotationProcessorTest {

    @Test
    public void testAllPrivateConstructors() {
        asssertCompilationSuccessful(compileTestCase("MySingleton"));

        assertCompilationReturned(Kind.ERROR, -1, compileTestCase("SingletonWithPublicConstructor"));
        //FIXME: -1? How to figure out
        assertCompilationReturned(Kind.ERROR, -1, compileTestCase("ExtendingSingleton"));
        assertCompilationReturned(Kind.ERROR, -1, compileTestCase("SingletonWithOnlyOneConstructor"));
    }

    @Test
    public void testAccessInjectedInstance() {
        assertCompilationSuccessful(compileTestCase(("MyBooks")));
    }

    @Override
    protected Collection<Processor> getProcessors() {
        return Arrays.<Processor>asList(new SingletonProcessor(), new InstanceProcessor(), new GetInstanceProcessor());
    }
}