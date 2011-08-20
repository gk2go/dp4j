package com.dp4j;

/**
 *
 * @author simpatico
 */
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
@Target(value={ElementType.CONSTRUCTOR,ElementType.METHOD})
public @interface TestPrivates {
    boolean catchExceptions() default false;
}
