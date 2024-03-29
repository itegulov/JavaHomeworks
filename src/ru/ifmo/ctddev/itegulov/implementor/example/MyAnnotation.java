package ru.ifmo.ctddev.itegulov.implementor.example;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Daniyar Itegulov
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface MyAnnotation {
}
