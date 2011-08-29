package com.dp4j.samples;

class PriClass {

    private int i;
    public int j;
    public static int gg;
    private Object[] objs = new Object[4];


    private void aPrivateMethod(int i, Double b, String... strings) {

    }

    private String aPrivateM(){
        return "";
    }
}

class K {

    protected int m;
    PriClass pc = new PriClass();

    private String getClassName() {
        return this.getClass().getCanonicalName();
    }

    private int get(int i){
        return i;
    }
}

public class CompTest extends K {

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
                if(kk == Integer.MAX_VALUE) System.out.println(get(3));
//                System.out.println(get(2) + pc.i);
            }
        }

        PriClass privateClass = new PriClass();
        int ff = privateClass.i;
        pi = new PriClass().i;
        int i = new PriClass().j;
//TODO:        new PriClass().aPrivateM().equals("");
        try {
            int kk = new PriClass().i;
            privateClass.aPrivateMethod(kk, new Double(5), "hello", "injected", "reflection");
        } catch (Exception e) {
            int kk = new PriClass().i;
        }
    }
}