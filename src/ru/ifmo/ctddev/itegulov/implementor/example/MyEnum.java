package ru.ifmo.ctddev.itegulov.implementor.example;

/**
 * @author Daniyar Itegulov
 */
public enum MyEnum {
    A(0), B(1), C(2);
    
    private int i;
    
    MyEnum(int i) {
        this.i = i;
    }

    public int getI() {
        return i;
    }
}
