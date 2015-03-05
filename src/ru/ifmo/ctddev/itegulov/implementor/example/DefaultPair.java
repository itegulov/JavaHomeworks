package ru.ifmo.ctddev.itegulov.implementor.example;

/**
 * @author Daniyar Itegulov
 */
public abstract class DefaultPair<A, B> implements Pair<A, B> {
    public DefaultPair(A a, B b) {
        this.a = a;
        this.b = b;
    }

    A a;
    B b;

    @Override
    public A getA() {
        return a;
    }

    @Override
    public B getB() {
        return b;
    }


    public abstract void ab(Class<? extends A>[] b);
}
