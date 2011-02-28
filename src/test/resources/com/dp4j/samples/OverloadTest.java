/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dp4j.samples;

import org.junit.Test;

class OverLoaded{
    private static void oM(){

    }

    private static void oM(final String s){

    }
}

/**
 *
 * @author simpatico
 */
public class OverloadTest {

    @Test
    public void test(){
        OverLoaded.oM();
        OverLoaded.oM("");
    }
}
