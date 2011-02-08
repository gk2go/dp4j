/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dp4j;

class PrivateClass{
    private void aPrivateMethod(int i){

    }
}

/**
 *
 * @author simpatico
 */
public class CallTest {

    @org.junit.Test
            public void callTest(){
                Object[] testArray = new Object[4];
                new PrivateClass().aPrivateMethod(5);
            }
}
