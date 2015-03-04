package ru.ifmo.ctddev.itegulov.implementor.example;

import ru.ifmo.ctddev.itegulov.implementor.example.MiddleClass;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniyar Itegulov
 * @since 03.03.15
 */
public  abstract class SecondMiddleClass extends MiddleClass {
    @Override
    public abstract int getHehInt();
    
    @Override
    public abstract HashMap getHehPizdosJava(Map map);
}
