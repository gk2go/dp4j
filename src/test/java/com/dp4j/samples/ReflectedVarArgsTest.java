/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dp4j.samples;

/**
 *
 * @author simpatico
 */
public class ReflectedVarArgsTest {

//    @org.junit.Test()
    public void varArgsTest() throws java.lang.ClassNotFoundException, java.lang.NoSuchFieldException, java.lang.IllegalAccessException, java.lang.reflect.InvocationTargetException, java.lang.IllegalArgumentException, java.lang.NoSuchMethodException {
        final java.lang.Class privateVarArgsClass = java.lang.Class.forName("com.dp4j.samples.PrivateVarArgs");
        final java.lang.reflect.Method aPrivateMethodMethod = privateVarArgsClass.getDeclaredMethod("aPrivateMethod", java.lang.Integer.TYPE, java.lang.Double.class, java.lang.String[].class);
        aPrivateMethodMethod.setAccessible(true);
        aPrivateMethodMethod.invoke(new PrivateVarArgs(), 4, new Double(3.0), new String[]{"hello", "injected", "reflection"});
    }
}