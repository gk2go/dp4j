/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.samples;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author simpatico
 */
public class CatchExceptionsTest {

    @Test
    @com.dp4j.Reflect(catchExceptions=true)
    public void test(){
        new PrivateMethods().getClassName();
    }

//    @Test
//    @com.dp4j.Reflect(catchExceptions=true)
//    public void testWithOwnTryCatchBlock(){
//        new PrivateMethods().getClassName();
//    }
}
