package com.dp4j.samples;

class PrivateClass {

    private int i;
    public int j;
    public static int gg;
    private Object[] objs = new Object[4];
}

class K {

    protected int m;
    PrivateClass pc = new PrivateClass();
}

public class Test extends K {

    int jj;
    int g;
    int h;

    @org.junit.Test
    public void t() {
        if (pc.i > 0) {
            int g = pc.i;
        }
        double pi = 3.14;
        final int fff = pc.i;
        {
            int f = pc.i;
        }

        while (2 < Math.random() * 5) {
            int kk = pc.i;
            for (Object object : pc.objs) {
               kk = pc.i;
                System.out.println(pc.i);
            }
        }

        PrivateClass privateClass = new PrivateClass();
        int ff = privateClass.i;
        pi = new PrivateClass().i;
        int i = new PrivateClass().j;

        try {
            int kk = new PrivateClass().i;
        } catch (Exception e) {
            int kk = new PrivateClass().i;
        }
    }
}