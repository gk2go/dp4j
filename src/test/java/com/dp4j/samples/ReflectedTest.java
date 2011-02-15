package com.dp4j.samples;

public class ReflectedTest extends WithAccessibleVarArgsInstance{

    public ReflectedTest() {
        super();
    }
    int jj;
    int g;
    int h;

//    @org.junit.Test()
    public void t() throws java.lang.ClassNotFoundException, java.lang.NoSuchFieldException, java.lang.IllegalAccessException, java.lang.reflect.InvocationTargetException, java.lang.IllegalArgumentException, java.lang.NoSuchMethodException {
        final java.lang.Class priClassClass = java.lang.Class.forName("com.dp4j.samples.PrivateVarArgs");
        final java.lang.reflect.Field iField = priClassClass.getDeclaredField("i");
        iField.setAccessible(true);
        if (((java.lang.Integer)iField.get(pc)) > 0) {
            int g = ((java.lang.Integer)iField.get(pc));
        }
        double pi = 3.14;
        final int fff = ((java.lang.Integer)iField.get(pc));
        {
            int f = ((java.lang.Integer)iField.get(pc));
        }
        while (2 < Math.random() * 5) {
            int kk = ((java.lang.Integer)iField.get(pc));
            final java.lang.reflect.Field objsField = priClassClass.getDeclaredField("objs");
            objsField.setAccessible(true);
            for (Object object : ((java.lang.Object[])objsField.get(pc))) {
                kk = ((java.lang.Integer)iField.get(pc));
                System.out.println(iField.get(pc));
            }
        }
        PrivateVarArgs privateClass = new PrivateVarArgs();
        int ff = ((java.lang.Integer)iField.get(privateClass));
        pi = ((java.lang.Integer)iField.get(new PrivateVarArgs()));
        int i = new PrivateVarArgs().j;
        try {
            int kk = ((java.lang.Integer)iField.get(new PrivateVarArgs()));
            final java.lang.reflect.Method aPrivateMethodMethod = priClassClass.getDeclaredMethod("aPrivateMethod", java.lang.Integer.TYPE, java.lang.Double.class, java.lang.String[].class);
            aPrivateMethodMethod.setAccessible(true);
            aPrivateMethodMethod.invoke(privateClass, kk, new Double(5), "hello", "injected", "reflection");
        } catch (Exception e) {
            int kk = ((java.lang.Integer)iField.get(new PrivateVarArgs()));
        }
    }
}