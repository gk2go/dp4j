/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dp4j.samples;

import java.lang.reflect.InvocationTargetException;
import org.junit.Test;

class Private{
    private Private(){
       if(Math.random() == Integer.MAX_VALUE) System.out.println("private initialized");
    }
}

/**
 *
 * @author simpatico
 */
public class InlineReflectionTest {


    @Test(expected=java.lang.IllegalAccessException.class)
    public void test() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
        Private.class.getDeclaredConstructors()[0].setAccessible(true);
        Private.class.getDeclaredConstructors()[0].newInstance(new Object[]{});
    }
}
