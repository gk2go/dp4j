/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.samples;

/**
 *
 * @author simpatico
 */
public class ObjectToTestDp4j {

    public int dummy;

    private int privateField;
    private ObjectToTestDp4j() { this.privateField = 123; }
    private int privateMethod() { return 456; }
}
