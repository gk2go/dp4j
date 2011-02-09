/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dp4j.samples;

/**
 *
 * @author simpatico
 */

class PrivateClass{
    private static int k;
}

public class SoutTest {

    @org.junit.Test
            public void test(){
                System.out.println(PrivateClass.k);
            }
}
