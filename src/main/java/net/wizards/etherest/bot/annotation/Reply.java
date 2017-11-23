package net.wizards.etherest.bot.annotation;

import net.wizards.etherest.bot.EtherListener;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Reply {
    EtherListener.Expect[] value();
}
