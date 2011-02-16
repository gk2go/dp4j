/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.samples;

import org.junit.Test;
import java.lang.*;
/**
 *
 * @author simpatico
 */
public class InvokeTest {

    @Test
    public void test() {
        System.console();
        System.out.println();
        System.out.getClass().getClassLoader().toString().contentEquals("cs");
        System.class.toString().contentEquals("cds");
        int[] ints = {4,32,34};
        System.out.println(ints);
    }
}
