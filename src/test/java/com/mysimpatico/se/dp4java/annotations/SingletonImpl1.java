/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mysimpatico.se.dp4java.annotations;

import com.mysimpatico.se.dp4java.annotations.singleton.*;

@Singleton
public class SingletonImpl1 {

    @instance
    private static SingletonImpl1 instance;

    @getInstance
    public static synchronized SingletonImpl1 getInstance() {
        if (instance == null) {
            instance = new SingletonImpl1();
        }
        return instance;
    }
}
