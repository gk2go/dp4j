/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.samples;

/**
 *
 * @author simpatico
 */
public class WithAccessibilePrivateDataInstance {

    protected int m;
    PrivateData pc = new PrivateData();

    private String getClassName() {
        return this.getClass().getCanonicalName();
    }
}
