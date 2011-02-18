
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.samples;

/**
 *
 * @author simpatico
 */
class PrivateArrayMethod {

    private void aPrivateMethod(String[] strings) {
    }

    private int anotherPrivateMethod(String[] strings, int[] ints, Double d) {
        return ints[0];
    }
}

public class ArrayCallTest {

    @org.junit.Test
    public void arrayTest() {
        new PrivateArrayMethod().aPrivateMethod(new String[]{"hello", "injected", "reflection"});
        new PrivateArrayMethod().anotherPrivateMethod(new String[]{"hello"}, new int[]{1, 2, 3}, new Double(4d));
    }
}
