package ru.ifmo.ctddev.itegulov.implementor.example;

import ru.ifmo.ctddev.itegulov.implementor.example.SecondMiddleClass;

import java.util.stream.Stream;

/**
 * @author Daniyar Itegulov
 * @since 28.02.15
 */
public abstract class GenericClassExample<E extends AutoCloseable & Stream> extends SecondMiddleClass implements Comparable<String> {
    public abstract <D> D getABC();
    
    public boolean isDaniyarLolka() {
        return true;
    }
}
