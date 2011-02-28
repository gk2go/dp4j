/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.samples;

import org.junit.Test;

/**
 *
 * @author simpatico
 */
public class MultipleCallsTest {

    @Test
    public void getPrivateReturn() {
        PrivateMethods privateClazzz = new PrivateMethods();
        String className = privateClazzz.getClassName();
        privateClazzz.aPrivateMethod(5, className);
        if(className.contains("        "))
            System.out.println(className);
    }
}
