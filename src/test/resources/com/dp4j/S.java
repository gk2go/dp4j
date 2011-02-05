/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j;


@Singleton
public class S {

    @instance
    private static S instance;

    @getInstance
    public static synchronized S getInstance() {
        SingletonImpl singletonImpl /*= new SingletonImpl()*/;
        if (instance == null) {
            instance = new S();
        }
        return instance;
    }
}

@Singleton
class S1 {

//    @getInstance
//    static synchronized S1 getInstance() {
////        if (instance == null) {
////            instance = new S1();
////        }
//        return new S1();
//    }
}
