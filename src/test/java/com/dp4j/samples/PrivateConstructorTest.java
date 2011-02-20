/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.samples;

import com.dp4j.samples.ASingleton;
import org.junit.Test;

/**
 *
 * @author simpatico
 */
public class PrivateConstructorTest {

    @Test
    public void test() {
        new ASingleton();
    }
//FIXME: injected code is not available to other processors already!
//    @Test
//    public void instanceTest() {
//        ASingleton.instance = null;
//    }
}
