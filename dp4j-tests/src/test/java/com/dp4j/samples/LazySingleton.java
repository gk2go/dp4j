package com.dp4j.samples;
import com.dp4j.*;

/**
 *
 * @author simpatico
 */
@Singleton(lazy=true)
public class LazySingleton {

    @instance
    public static LazySingleton instance;
}