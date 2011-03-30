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

public class ParsingTest {



    @Test
    @Reflect(catchExceptions=true, all=true)
    public void setValue(PrivateData pd, int value, int a){
        pd.i = value;
    }

    @Reflect(catchExceptions=false)
    public void setVal(Object o, int value){
        int j = value;
    }
}
