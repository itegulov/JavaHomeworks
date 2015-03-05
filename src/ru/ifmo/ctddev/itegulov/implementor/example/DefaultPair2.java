package ru.ifmo.ctddev.itegulov.implementor.example;

import java.util.Set;

public abstract class DefaultPair2<F> extends DefaultPair<F,Set<Integer>> {

    public DefaultPair2(F f, Set<Integer> s) {
        super(f, s);
    }
}
