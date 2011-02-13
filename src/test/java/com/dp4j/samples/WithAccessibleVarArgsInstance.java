/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.samples;

/**
 *
 * @author simpatico
 */
public class WithAccessibleVarArgsInstance {

    protected int m;
    PrivateVarArgs pc = new PrivateVarArgs();

    private String getClassName() {
        return this.getClass().getCanonicalName();
    }
}
