package com.dp4j.samples;

class RPrivateArrayMethod {

    private void aPrivateMethod(String[] strings) {
    }
}
public class ReflectedArrayCallTest {

//    @org.junit.Test()
    public void arrayTest() throws java.lang.ClassNotFoundException, java.lang.NoSuchFieldException, java.lang.IllegalAccessException, java.lang.reflect.InvocationTargetException, java.lang.IllegalArgumentException, java.lang.NoSuchMethodException {
        final java.lang.Class privateArrayMethodClass = java.lang.Class.forName("com.dp4j.samples.RPrivateArrayMethod");
        final java.lang.reflect.Method aPrivateMethodMethod = privateArrayMethodClass.getDeclaredMethod("aPrivateMethod", java.lang.String[].class);
        aPrivateMethodMethod.setAccessible(true);
        aPrivateMethodMethod.invoke(new RPrivateArrayMethod(), new Object[]{new String[]{"hello", "injected", "reflection"}});
        }
}