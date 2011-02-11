/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.samples;

import org.junit.Test;

/**
 *
 * @author simpatico
 */
class PrivateClazzz {

    private void aPrivateMethod(int i, String b) {

    }

    private String getClassName() {
        return this.getClass().getCanonicalName();
    }
}

public class MultipleCallsTest {

    @Test
    public void getPrivateReturn() {
        PrivateClazzz privateClazzz = new PrivateClazzz();
        String className = privateClazzz.getClassName();
        privateClazzz.aPrivateMethod(5, className);
        System.out.println(className);
    }
}
