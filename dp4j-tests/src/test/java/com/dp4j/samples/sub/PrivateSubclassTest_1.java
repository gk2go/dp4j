/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.samples.sub;

import com.dp4j.samples.PrivateData;
import org.junit.Test;

/**
 *
 * @author simpatico
 */
public class PrivateSubclassTest_1 {

    @Test
    public void setProtectedField() {
        PrivateData.CONSTANT_S = "Reflection";
    }
}
