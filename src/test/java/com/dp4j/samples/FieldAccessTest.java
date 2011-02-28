package com.dp4j.samples;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author simpatico
 */
public class FieldAccessTest {


    @org.junit.Test
    public void t(){
                PrivateData privateClass = new PrivateData();
        int ff = privateClass.i;
    }
}
