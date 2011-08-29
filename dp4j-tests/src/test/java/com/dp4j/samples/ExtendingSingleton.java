/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dp4j.samples;

import com.dp4j.Singleton;
import com.dp4j.getInstance;
import com.dp4j.instance;

class NonSingleton {

    public NonSingleton() {
    }

    public NonSingleton(final String a) {
    }
}

@Singleton
public class ExtendingSingleton extends NonSingleton {

    @instance
    private static final ExtendingSingleton instance = new ExtendingSingleton();

    public final void doSomething() {
    }

    private ExtendingSingleton() {
        super();
        doSomething();
    }

//    public ExtendingSingleton(final String a) {
//        super(a);
//    }

    /**
     * @return the instance
     */
    @getInstance
    public static ExtendingSingleton getInstance() {
        return instance;
    }
}