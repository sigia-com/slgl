package io.slgl.client.utils;


import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
public @interface Experimental {
    String value() default "";
}
