package ru.ifmo.ctddev.itegulov.implementor.test;

import org.junit.Test;
import ru.ifmo.ctddev.itegulov.implementor.example.*;

import java.util.Comparator;

/**
 * @author Daniyar Itegulov
 * @since 05.03.15
 */
public class MyImplementorTest extends ClassImplementorTest {
    @Test
    public void test16_randomBigInterface() {
        test(false, ExampleInterface.class, AnotherInterface.class);
    }

    @Test
    public void test17_someBigAbstractClass() throws Exception {
        test(false, ExampleAbstractClass.class);
    }

    @Test
    public void test18_genericClass() throws Exception {
        test(false, GenericClassExample.class);
    }

    @Test
    public void test19_pairs() throws Exception {
        test(false, Pair.class, DefaultPair.class, DefaultPair2.class, DefaultPair3.class);
    }

    @Test
    public void test20_myMap() throws Exception {
        test(false, MyMap.class);
    }

    @Test
    public void test21_internalInterface() throws Exception {
        test(false, SimpleTemplateInterface.class);
    }

    @Test
    public void test22_annotation() throws Exception {
        test(false, MyAnnotation.class);
    }

    @Test
    public void test23_enum() throws Exception {
        test(true, MyEnum.class);
    }

    @Test
    public void test24_anonymousClass() throws Exception {
        test(true, ((Comparator<Integer>) (o1, o2) -> 0).getClass());
    }

    @Test
    public void test25_overridenAbstractMethod() throws Exception {
        test(false, SimpleClassExtended.class);
    }
}
