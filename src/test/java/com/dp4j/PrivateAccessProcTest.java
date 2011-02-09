/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j;

import com.dp4j.processors.core.PrivateAccessProcessor;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import javax.annotation.processing.Processor;

import org.junit.Test;
/**
 *
 * @author simpatico
 */
public class PrivateAccessProcTest extends AbstractAnnotationProcessorTest {

    @org.junit.Test
    public void mostComprehensiveTest() throws IOException {

        assertCompilationSuccessful(compileTestCase("Test"));
    }

    @Test
    public void testCallingPrivateMethod(){
        asssertCompilationSuccessful(compileTestCase("CallTest"));
    }

    @Test
    public void testPrivateParams(){
        asssertCompilationSuccessful(compileTestCase("SoutTest"));
    }

    @Test
    public void testSettingValues(){
        asssertCompilationSuccessful(compileTestCase("IfTest"));
    }

    @Override
    protected Collection<Processor> getProcessors() {
        return Arrays.<Processor>asList(new PrivateAccessProcessor());
    }
}
