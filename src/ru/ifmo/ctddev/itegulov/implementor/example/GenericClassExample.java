package ru.ifmo.ctddev.itegulov.implementor.example;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author Daniyar Itegulov
 */
public abstract class GenericClassExample<E extends AutoCloseable & Stream, L> extends SecondMiddleClass implements Comparable<String[]> {
    public GenericClassExample(E[] array, E value, Set<? extends E> set, L l, L l2, L... args) {

    }

    public abstract <D> List<? super D> getABC();
    
    public abstract E getE();
    
    protected abstract List<E> returnList();
    
    public abstract <D, T extends E> void foo(List<? extends D> list, Map<? super E, ? extends T> map);
    
    public boolean isDaniyarLolka() {
        return true;
    }
    
    protected abstract void example();
}
