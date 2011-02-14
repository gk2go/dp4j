/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dp4j.samples;

/**
 *
 * @author simpatico
 */

public class VarArgsCallTest {

    @org.junit.Test
    public void varArgsTest(){
        new PrivateVarArgs().aPrivateMethod(4,new Double(3d), "hello", "injected", "reflection");
    }
}
