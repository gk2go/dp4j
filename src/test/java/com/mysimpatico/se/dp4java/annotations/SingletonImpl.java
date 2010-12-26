/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.mysimpatico.se.dp4java.annotations;
import com.mysimpatico.se.dp4java.annotations.singleton.*;

  @Singleton
  public class SingletonImpl{

	@instance
        private static SingletonImpl instance;

	private SingletonImpl() {}

        @getInstance
	public static synchronized SingletonImpl getInstance() {
		if (instance == null) {
			instance = new SingletonImpl();
		}
		return instance;
	}
    }
