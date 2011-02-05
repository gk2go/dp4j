/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author simpatico
 */
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

public class IfTest extends K{

    @org.junit.Test
    public void t() {
        if (pc.i > 0) {
            int i = pc.i;
            int f = pc.i;
            m = pc.i;
            pc.i = 5;
        }
    }
}
