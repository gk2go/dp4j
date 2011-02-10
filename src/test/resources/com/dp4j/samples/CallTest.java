/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dp4j.samples;

class PrivateClazz{
    private void aPrivateMethod(int i){

    }
}

/**
 *
 * @author simpatico
 */
public class CallTest {

    @org.junit.Test
            public void callTest() throws Exception{
                Object[] testArray = new Object[4];
                new PrivateClazz().aPrivateMethod(5);
            }
}
