/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dp4j;

/**
 *
 * @author Gabriele
 */
@Singleton
public class SS {

    int index = 5;
    public void he(){

    }

    private SS(int i){

    }

    @instance
    public static final SS ssInst = new SS(5);
}
