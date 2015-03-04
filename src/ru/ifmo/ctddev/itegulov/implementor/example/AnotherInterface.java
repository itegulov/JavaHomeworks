package ru.ifmo.ctddev.itegulov.implementor.example;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.stream.BaseStream;

public interface AnotherInterface {
    @Deprecated
    public boolean getBoolean(Boolean b, boolean a) throws IndexOutOfBoundsException;
    public int getInt();
    public char getChar();
    public String getString();
    public List<Integer> getList();
    public List<PrintWriter> getPrintWriter();
    public Map<? extends List, AutoCloseable> getStrangeMap();
    public List<?>[] getEdges();
    public List<? extends BaseStream> getStreamList();
    public ExampleInterface getThis(Boolean bool, boolean b, int i, Integer INT, List<? extends PrintWriter>[][] x);
    public void reverse();

}