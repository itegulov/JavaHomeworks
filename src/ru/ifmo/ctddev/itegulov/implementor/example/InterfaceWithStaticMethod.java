package ru.ifmo.ctddev.itegulov.implementor.example;

/**
 * @author Georgiy Korneev (kgeorgiy@kgeorgiy.info)
 */
public interface InterfaceWithStaticMethod {
    int hello();
    static void staticMethod() {
        System.out.println("staticMethod");
    }
}