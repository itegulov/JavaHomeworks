package ru.ifmo.ctddev.itegulov.implementor.example;

import java.io.IOException;
import java.nio.file.NoSuchFileException;

/**
 * @author Daniyar Itegulov
 * @since 28.02.15
 */
public abstract class ExampleAbstractClass extends MiddleClass {
    public ExampleAbstractClass(int a, int b) throws IOException {
        throw new IOException("HEH");
    }

    public ExampleAbstractClass() throws NoSuchFileException {
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
