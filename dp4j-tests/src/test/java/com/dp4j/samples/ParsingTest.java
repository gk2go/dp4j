/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dp4j.samples;

import com.dp4j.Reflect;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;

/**
 *
 * @author simpatico
 */

public class ParsingTest {

    @Reflect(catchExceptions=true, all=true)
    public void setValue(PrivateData pd, int value, int a){
        pd.i = value;
    }

    @Reflect(catchExceptions=false)
    public void setVal(Object o, int value){
        int j = value;
    }

    @Test
    public void test(){
        setValue(new PrivateData(), 4, 5);
        setVal(null,5);
    }

    @Test
    @Reflect(all=true)
    public void reflectAllSetterTest(){
        final PrivateData pd = new PrivateData();
        final int value = 5;
        pd.i = value;
    }

    @Test
    @Reflect(catchExceptions=true)
    public void reflectSetterCatchTest(){
        final PrivateData pd = new PrivateData();
        final int value = 5;
        pd.i = value;
    }

    @Test
    @Reflect(catchExceptions=true)
    public void reflectInvokeCatchTest(){
        final PrivateMethods pm = new PrivateMethods();
        pm.getClassName();
    }

    @Test
    @Reflect(catchExceptions=true)
    public void reflectInvokeCatchTest2(){
        final PrivateMethods pm = new PrivateMethods();
        String className;
        className = pm.getClassName();
        final byte[] bytes = null;
        final int[] ints;
            if (bytes == null) {
                ints = null;
            } else {
                ints = new int[5];
            }
    }

//    @Test
//    public void dp4j4Test() throws Exception{
//     Thread.sleep(1);
//    }

    @Test
    @Reflect(all=true)
    public void dp4j1Test(){
        Logger.getLogger(ParsingTest.class.getName()).log(Level.FINE,"");
    }
}
