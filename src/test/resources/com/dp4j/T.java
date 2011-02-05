package com.dp4j;

class PrivateClass {

    private int i;
    public int j;
    public static int gg;
}

class K{
    protected int m;
    PrivateClass pc = new PrivateClass();
}

public class T extends K{

    int jj;
    int g;
    int h;

    @org.junit.Test
    public void t() {
        //         double j = 2.2250738585072012e-308;
        //        int pi = 3.14;
        final int fff = pc.i;
        PrivateClass privateClass = new PrivateClass();
        int ff = privateClass.i;
         int pi = new PrivateClass().i;
         int i  = new PrivateClass().j;

        try {
            int kk = new PrivateClass().i;
        } catch (Exception e) {
        }
    }
}