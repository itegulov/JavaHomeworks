package ru.ifmo.ctddev.itegulov.implementor;

import ru.ifmo.ctddev.itegulov.arrayset.ArraySet;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.NoSuchFileException;
import java.util.*;
import java.util.stream.Stream;

class ExampleAbstractClassImpl extends ExampleAbstractClass {
    public ExampleAbstractClassImpl() throws NoSuchFileException {
        super();
    }
    public ExampleAbstractClassImpl(int a, int b) throws IOException {
        super(a, b);
    }

    @Override
    protected void doNothing() {
    }

    @Override
    public Set<Integer> getSet() {
        return null;
    }

    @Override
    public Map<?, String> getStrangeMap() {
        return null;
    }

    @Override
    public int getInt() {
        return 0;
    }

    @Override
    public Set<?> getWildcardSet() {
        return null;
    }

    @Override
    public Boolean getObjectBoolean(boolean bool) {
        return null;
    }

    @Override
    public Object getObject() {
        return null;
    }

    @Override
    protected void testMethod() {
    }

    @Override
    public Map<?, ?>[][][][][][][][][][][][][][][][][][][][][][][][][][] emmm() {
        return null;
    }

    @Override
    public Set<PrintWriter> getPrintWriters() {
        return null;
    }

    @Override
    public void someProcedure(int a, double b, boolean c, Object d) {
    }

    @Override
    public void nothing() {
    }

    @Override
    protected void testMethod(double d) {
    }

    @Override
    public void procedure(Object o) {
    }

    @Override
    public double getDouble() {
        return 0;
    }

    @Override
    public Map<AbstractSet, ? extends AbstractMap>[] getBadMaps() {
        return null;
    }

    @Override
    public Set<? extends ArrayList<? super LinkedList<? extends ArraySet<? super Stream<? extends Integer>>>>> getALotOfWildCards() {
        return null;
    }

    @Override
    protected int testMethod(int i, int j) {
        return 0;
    }

    @Override
    public String getFromMany(String[] games) {
        return null;
    }

    @Override
    public Map<Integer, String> getCoolMap() {
        return null;
    }

    @Override
    public boolean getBoolean() {
        return false;
    }

    @Override
    protected void testMethod(int i) {
    }

    @Override
    public Map<? extends List<? super Collection>, AutoCloseable> getHehMap() {
        return null;
    }

}