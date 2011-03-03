/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dp4j;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
/**
 *
 * @author simpatico
 */
@Target(value={ElementType.CONSTRUCTOR,ElementType.METHOD})
public @interface Reflect {
    boolean all() default false;
            }

