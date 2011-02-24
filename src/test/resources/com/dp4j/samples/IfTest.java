/**
 *
 *
 * @author simpatico
 */
package com.dp4j.samples;

public class IfTest extends WithAccessibilePrivateDataInstance{

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
