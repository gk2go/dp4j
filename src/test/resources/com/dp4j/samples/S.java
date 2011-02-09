/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.samples;

import com.dp4j.*;


@Singleton
public class S {

    @instance
    private static S instance;

    @getInstance
    public static synchronized S getInstance() {
        SingletonImpl singletonImpl;
        if (instance == null) {
            instance = new S();
        }
        return instance;
    }
}

@Singleton
class S1 {
}
