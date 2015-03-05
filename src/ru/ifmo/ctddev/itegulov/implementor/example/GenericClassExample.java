package ru.ifmo.ctddev.itegulov.implementor.example;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author Daniyar Itegulov
 * @since 28.02.15
 */
public abstract class GenericClassExample<E extends AutoCloseable & Stream> extends SecondMiddleClass implements Comparable<String> {
    public abstract <D> D getABC();
    
    public abstract E getE();
    
    protected abstract List<E> returnList();
    
    public abstract <D, T extends E> void foo(List<? extends D> list, Map<? super E, ? extends T> map);
    
    public boolean isDaniyarLolka() {
        return true;
    }
    
    protected abstract void example();
}
