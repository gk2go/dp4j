/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.samples;

/**
 *
 * @author simpatico
 */
public class K {
    protected int m;
    PriClass pc = new PriClass();

    private String getClassName() {
        return this.getClass().getCanonicalName();
    }

    private int get(int i){
        return i;
    }
}
