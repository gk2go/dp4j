/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.samples;

import com.dp4j.AbstractAccessibleTest;

/**
 *
 * @author simpatico
 */
public class AccessibleTest extends AbstractAccessTest {

    @org.junit.Test
    public void test() {
        compileTestCase("This is an accessible method invokation");
    }
}
