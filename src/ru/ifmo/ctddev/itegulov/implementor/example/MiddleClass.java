package ru.ifmo.ctddev.itegulov.implementor.example;

import java.util.Map;

/**
 * @author Daniyar Itegulov
 * @since 28.02.15
 */
public abstract class MiddleClass implements ExampleInterface {
    
    public abstract boolean isDaniyarLolka();
    
    protected abstract void testMethod();

    protected abstract void testMethod(int i);

    protected abstract void testMethod(double d);

    protected abstract int testMethod(int i, int j);
    
    //@Override
    //public abstract AbstractSet<Integer> getSet();
    
    public int getHehInt() {
        return 0;
    }
    
    public abstract Map getHehPizdosJava(Map map);
    
    protected abstract Map<Integer, Integer> wut();
}
