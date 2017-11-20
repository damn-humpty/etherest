package net.wizards.etherest.annotation;

import net.wizards.etherest.http.Request;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestMapping {
    String[] value();
    Request.Method[] method() default { Request.Method.POST, Request.Method.GET };
    String produces() default "application/json";
    String[] params() default {};
}
