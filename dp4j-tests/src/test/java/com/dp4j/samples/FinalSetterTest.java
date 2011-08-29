/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dp4j.samples;

import com.dp4j.samples.sub.PrivateSubclass;
import org.junit.Test;


/**
 *
 * @author simpatico
 */
public class FinalSetterTest {

    @Test
    public void test(){
        PrivateData.CONSTANT = 1;
        new PrivateData().CONSTANT_O = new Object();
    }

}
