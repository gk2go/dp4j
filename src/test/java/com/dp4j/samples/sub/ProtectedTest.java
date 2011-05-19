/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.samples.sub;

import com.dp4j.samples.ProtectedClass;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author simpatico
 */
public class ProtectedTest {

    @Test
    public void test() {
        new ProtectedClass().doSth();
    }
}
