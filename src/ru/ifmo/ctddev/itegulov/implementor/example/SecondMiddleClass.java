package ru.ifmo.ctddev.itegulov.implementor.example;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniyar Itegulov
 */
public  abstract class SecondMiddleClass extends MiddleClass {
    @Override
    public abstract int getHehInt();
    
    @Override
    public abstract HashMap getHehPizdosJava(Map map);

    @Override
    protected abstract HashMap<Integer, Integer> wut();
}
