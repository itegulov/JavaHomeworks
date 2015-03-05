package ru.ifmo.ctddev.itegulov.implementor.example;

import java.nio.file.NoSuchFileException;

/**
 * @author Daniyar Itegulov
 */
public abstract class ExampleAbstractClass extends MiddleClass {
    protected ExampleAbstractClass() throws NoSuchFileException {
        super();
    }

    public final boolean returnTrue() {
        return true;
    }
    
    protected abstract void doNothing();
    
    public abstract void procedure(Object o);

    public abstract int getInt();

    @Deprecated
    public abstract double getDouble();

    public abstract Object getObject();

    public abstract boolean getBoolean();

    public abstract Boolean getObjectBoolean(boolean bool);

    public abstract void someProcedure(int a, double b, boolean c, Object d);

    public abstract void nothing();
}
