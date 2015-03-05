package ru.ifmo.ctddev.itegulov.implementor.example;

import java.util.HashMap;
import java.util.HashSet;

/**
 * @author Daniyar Itegulov
 */
public abstract class SimpleClassExtended extends SimpleClass {
    @Override
    public abstract HashMap map();

    @Override
    protected abstract HashSet set();

    @Override
    protected abstract HashMap<Integer, Integer> wut();
}
