/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dp4j.samples;

/**
 *
 * @author simpatico
 */
class Genericer{
    private static<T> String getAll(T t){
        return null;
    }

    void test(){
        getAll(new String());
    }
}