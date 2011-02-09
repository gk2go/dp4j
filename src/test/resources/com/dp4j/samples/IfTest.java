/**
 *
 *
 * @author simpatico
 */
package com.dp4j.samples;


class PrivateClass {

    private int i;
    public int j;
    public static int gg;
}

class K{
    protected int m;
    PrivateClass pc = new PrivateClass();
}

public class IfTest extends K{

    @org.junit.Test
    public void t() {
        if (pc.i > 0) {
            int i = pc.i;
            int f = pc.i;
            m = pc.i;
            pc.i = 5; //should fail here, until setValue is supported
        }
    }
}
