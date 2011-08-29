/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dp4j.samples;

import com.dp4j.Reflect;
import org.junit.Test;

/**
 *
 * @author simpatico
 */
public class ReflectedForEachTest {

    @Reflect
    public void t() throws java.lang.ClassNotFoundException, java.lang.NoSuchFieldException, java.lang.IllegalAccessException {
        final java.lang.reflect.Field staticObjsField = Class.forName("com.dp4j.samples.PrivateData").getDeclaredField("staticObjs");
        staticObjsField.setAccessible(true);
        for (Object staticObj : (java.lang.Object[])staticObjsField.get("")) {
            System.out.println(staticObj);
        }
        final java.lang.reflect.Field intsField = Class.forName("com.dp4j.samples.PrivateData").getDeclaredField("ints");
        intsField.setAccessible(true);

        for (int i : ((int[])intsField.get(new PrivateData())) ) {
            System.out.println(i);
        }
    }
}