/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dp4j.samples;

import com.dp4j.Reflect;
import org.junit.Test;

/**
 *
 * @author simpatico
 */
public class StaticTest {

    @Test
    @Reflect(all=true)
    public void test(){
        PrivateData.gg = 4;
    }
}
