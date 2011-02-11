/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dp4j.samples;

/**
 *
 * @author simpatico
 */

class PriClass {
    private void aPrivateMethod(int a, String... strings) {

    }
}

public class VarArgsCallTest {

    @org.junit.Test
    public void varArgsTest(){
        new PriClass().aPrivateMethod(4, "hello", "injected", "reflection");
    }
}
