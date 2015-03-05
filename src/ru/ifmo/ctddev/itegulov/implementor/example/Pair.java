package ru.ifmo.ctddev.itegulov.implementor.example;

/**
 * @author Daniyar Itegulov
 */
public interface Pair<F,S> {
    F getA();
    S getB();
    Pair<S,F> reverse();

    public abstract void aa(Class<? extends S>[] b);
}
