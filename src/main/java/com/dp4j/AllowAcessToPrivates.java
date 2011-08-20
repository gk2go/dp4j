/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j;

/**
 *
 * @author simpatico
 */
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
@Target(value={ElementType.CONSTRUCTOR,ElementType.METHOD})
public @interface AllowAcessToPrivates {
    boolean catchExceptions() default false;
}
