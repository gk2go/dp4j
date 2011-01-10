package com.mysimpatico.se.dp4java.annotations;
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import com.mysimpatico.se.dp4java.annotations.singleton.*;

/**
 *
 * @author Gabriele
 */
@com.mysimpatico.se.dp4java.annotations.singleton.Singleton
public class MyBooks {
private int year; //fields
    private String title;
    private String author;


    public void hello(){
		instance = null;	
    }
}
