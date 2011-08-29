/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dp4j.samples;

/**
 *
 * @author simpatico
 */
public class CallTest {

    @org.junit.Test
            public void callTest() throws Exception{
                Object[] testArray = new Object[4];
                new PrivateMethods().aPrivateMethod(5);
            }
}
