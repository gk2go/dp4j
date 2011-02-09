/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.samples;

import com.dp4j.*;

@Singleton
public class SingletonImpl {

    @instance
    private static SingletonImpl instance;

    private SingletonImpl() {
    }

    @getInstance
    public static synchronized SingletonImpl getInstance() {
        if (instance == null) {
            instance = new SingletonImpl();
        }
        return instance;
    }
}
