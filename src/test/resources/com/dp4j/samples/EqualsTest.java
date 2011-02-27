/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.samples;

import org.junit.Test;

/**
 *
 * @author simpatico
 */
public class EqualsTest {

    @Test
    public void test() {
        final int[] d = {1};
        boolean b = new Object().equals(null);
        Object[] arr = {null, ""};
        Object o = arr[1];
        d.equals("");
    }

}
