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
public class ForEachTest {

    @Test
    public void t() throws Exception {
//        for(Object staticObj: PrivateData.staticObjs){
//            System.out.println(staticObj);
//        }
        for(int i: new PrivateData().ints){
            System.out.println(i);
        }
    }
}
