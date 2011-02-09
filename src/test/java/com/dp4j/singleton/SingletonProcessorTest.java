/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.singleton;

import org.junit.Test;


import javax.tools.Diagnostic.Kind;

/**
 *
 * @author simpatico
 */
public class SingletonProcessorTest extends AbstractSingletonProcTest {

    @Test
    public void testAllPrivateConstructors() {
        asssertCompilationSuccessful(compileTestCase("MySingleton"));

        assertCompilationReturned(Kind.ERROR, -1, compileTestCase("SingletonWithPublicConstructor"));
        //FIXME: -1? How to figure out
        assertCompilationReturned(Kind.ERROR, -1, compileTestCase("ExtendingSingleton"));
        assertCompilationReturned(Kind.ERROR, -1, compileTestCase("SingletonWithOnlyOneConstructor"));
    }

    @Test
    public void testAccessInjectedInstance(){
        assertCompilationSuccessful(compileTestCase(("MyBooks")));
    }
}