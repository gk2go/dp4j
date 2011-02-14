
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
}

public class ArrayCallTest {

    @org.junit.Test
    public void arrayTest(){
        new PrivateArrayMethod().aPrivateMethod(new String[]{"hello", "injected", "reflection"});
    }
}

