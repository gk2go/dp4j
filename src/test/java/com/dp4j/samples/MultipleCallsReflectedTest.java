/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.samples;

import org.junit.Test;
import com.dp4j.samples.*;
/**
 *
 * @author simpatico
 */
public class MultipleCallsReflectedTest {

    @Test() //some issues actually finding the class. Maybe because in same package?
    public void getPrivateReturn() throws java.lang.ClassNotFoundException, java.lang.NoSuchFieldException, java.lang.IllegalAccessException, java.lang.reflect.InvocationTargetException, java.lang.IllegalArgumentException, NoSuchMethodException {
        PrivateMethods privateClazzz = new PrivateMethods();
        System.out.println(PrivateMethods.class.getCanonicalName());
        final java.lang.Class privateClazzzClass = java.lang.Class.forName("com.dp4j.samples.PrivateMethods");
        final java.lang.reflect.Method getClassNameMethod = privateClazzzClass.getDeclaredMethod("getClassName");
        getClassNameMethod.setAccessible(true);
        String className = ((java.lang.String) getClassNameMethod.invoke(privateClazzz));
        final java.lang.reflect.Method aPrivateMethodMethod = privateClazzzClass.getDeclaredMethod("aPrivateMethod", Integer.TYPE, java.lang.String.class);
        aPrivateMethodMethod.setAccessible(true);
        aPrivateMethodMethod.invoke(privateClazzz, 5, className);
        System.out.println(className);
    }
}
