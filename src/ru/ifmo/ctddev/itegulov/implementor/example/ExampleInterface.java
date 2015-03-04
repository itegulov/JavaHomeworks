package ru.ifmo.ctddev.itegulov.implementor.example;

import ru.ifmo.ctddev.itegulov.arrayset.ArraySet;

import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Stream;

/**
 * @author Daniyar Itegulov
 * @since 28.02.15
 */
public interface ExampleInterface {
    int getInt();
    
    @Deprecated
    double getDouble();
    
    Object getObject();

    boolean getBoolean();
    
    Boolean getObjectBoolean(boolean bool);
    
    void someProcedure(int a, double b, boolean c, Object d);
    
    Set<Integer> getSet();
    
    Set<?> getWildcardSet();

    Map<? extends List<? super Collection>, AutoCloseable> getHehMap();
    
    Map<?, String> getStrangeMap();
    
    Map<? , ?>[][][][][][][][][][][][][][][][][][][][][][][][][][] emmm();
    
    Map<AbstractSet, ? extends AbstractMap>[] getBadMaps();
    
    Set<? extends ArrayList<? super LinkedList<? extends ArraySet<? super Stream<? extends Integer>>>>> getALotOfWildCards();
    
    Set<PrintWriter> getPrintWriters();
    
    String getFromMany(String... games);
    
    Map<Integer, String> getCoolMap();
    
    void nothing();
}
