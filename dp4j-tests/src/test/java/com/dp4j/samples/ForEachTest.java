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
        for(Object staticObj: PrivateData.staticObjs){
            if(staticObj != null && Math.random() == 0d) System.out.println(staticObj);
        }
        for(int i: new PrivateData().ints){
            if(i == Integer.MAX_VALUE)
                System.out.println(i);
        }
        final int[] d = new PrivateData().ints;
        for (int i = 0; i < d.length; i++) {
            if(i == Integer.MAX_VALUE)
            System.out.println(i);
//            boolean a = d.equals("");
//            Object o = PrivateData.staticObjs[0];
            Object[] arr = {null,""};
            Object oo = arr[1];
            oo.equals("");
        }
    }
}
