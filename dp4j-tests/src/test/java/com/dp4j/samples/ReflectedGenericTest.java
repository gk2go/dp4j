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
public class ReflectedGenericTest {

    @Test
    public void test() throws Exception {
        final java.lang.reflect.Method[] getAllMethod = Class.forName("com.dp4j.samples.Genericer").getDeclaredMethods();
        final java.lang.reflect.Method getAllMethfod = Class.forName("com.dp4j.samples.Genericer").getDeclaredMethod("getAll", java.lang.Object.class);
//        getAllMethod.setAccessible(true);
//        getAllMethod.invoke(this, args);
    }
}
