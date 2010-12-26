/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mysimpatico.se.dp4java.annotations;

import com.mysimpatico.se.dp4java.annotations.singleton.*;

@Singleton
public class S {

    @instance
    private static S instance;

    @getInstance
    public static synchronized S getInstance() {
        SingletonImpl singletonImpl = new SingletonImpl();
        if (instance == null) {
            instance = new S();
        }
        return instance;
    }
}
