package ru.ifmo.ctddev.itegulov.implementor.example;

/**
 * @author Georgiy Korneev (kgeorgiy@kgeorgiy.info)
 */
public interface InterfaceWithDefaultMethod {
    int hello();
    default void defaultMethod() {
        System.out.println("defaultMethod");
    }
}