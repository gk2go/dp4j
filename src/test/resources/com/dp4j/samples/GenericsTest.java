/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dp4j.samples;

import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author simpatico
 */

class Genericer{
    private static<T> String getAll(T t){
        return null;
    }

    void test(){
        getAll(new String());
    }
}

public class GenericsTest {

    @Test
    public void test(){
          Assert.assertTrue(Genericer.getAll(String.class) == null);
    }
}