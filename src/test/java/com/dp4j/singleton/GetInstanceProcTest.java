/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.singleton;

import com.dp4j.processors.core.PrivateAccessProcessor;
import org.junit.Test;

/**
 *
 * @author simpatico
 */
public class GetInstanceProcTest extends AbstractSingletonProcTest {

    @Test
    public void testGetInstanceValidation() {
        asssertCompilationSuccessful(compileTestCase("S"));
        asssertCompilationSuccessful(compileTestCase("SS"));
        asssertCompilationSuccessful(compileTestCase("SingletonImpl"));
        asssertCompilationSuccessful(compileTestCase(getProcessors(new PrivateAccessProcessor()),"SingletonTest"));
    }
}
