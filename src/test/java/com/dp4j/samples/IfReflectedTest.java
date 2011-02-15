/**
 *
 *
 * @author simpatico
 */
package com.dp4j.samples;

public class IfReflectedTest extends WithAccessibilePrivateDataInstance {

//    @org.junit.Test
    public void t() throws java.lang.ClassNotFoundException, java.lang.NoSuchFieldException, java.lang.IllegalAccessException {
        final java.lang.Class privateClassClass = java.lang.Class.forName("com.dp4j.samples.PrivateData");
        final java.lang.reflect.Field iField = privateClassClass.getDeclaredField("i");
        iField.setAccessible(true);
        if ((Integer) iField.get(pc) > 0) {
            int i = ((java.lang.Integer) iField.get(pc));
            int f = ((java.lang.Integer) iField.get(pc));
            m = ((java.lang.Integer) iField.get(pc));
            iField.set(pc, 5);
        }
    }
}
