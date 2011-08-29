/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.samples;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author simpatico
 */
public class TestAcessingPrivateObject {

    @Test
    public void testAcessingObjectDP4J() throws Exception {
        ObjectToTestDp4j testObject = new ObjectToTestDp4j();
        assertEquals(123, testObject.privateField);
//        assertEquals(456, testObject.privateMethod());
    }
}
