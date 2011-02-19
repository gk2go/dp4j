/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dp4j.samples;

import com.dp4j.InjectReflection;
import java.lang.reflect.Method;

/**
 *
 * @author simpatico
 */

class Private{
     private // as mentioned by Bozho
    void foo(String... s) {
        System.err.println(s[0]);
    }
}
public class StackOverFlowTest {

    @org.junit.Test
    public void te() throws Exception {
        new StackOverFlowTest().m();
    }

  
    @InjectReflection
    void m() throws Exception {
        new Private().foo("hello", "kitty");
    }
}
