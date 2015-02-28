package ru.ifmo.ctddev.itegulov.implementor;

import java.nio.file.NoSuchFileException;

/**
 * @author Daniyar Itegulov
 * @since 28.02.15
 */
public abstract class MiddleClass implements ExampleInterface {
    public MiddleClass() throws NoSuchFileException {
        throw new NoSuchFileException("nope");
    }
    
    protected abstract void testMethod();

    protected abstract void testMethod(int i);

    protected abstract void testMethod(double d);

    protected abstract int testMethod(int i, int j);
}
