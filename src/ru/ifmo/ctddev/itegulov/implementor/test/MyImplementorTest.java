package ru.ifmo.ctddev.itegulov.implementor.test;

import org.junit.Test;
import ru.ifmo.ctddev.itegulov.implementor.example.AnotherInterface;
import ru.ifmo.ctddev.itegulov.implementor.example.ExampleAbstractClass;
import ru.ifmo.ctddev.itegulov.implementor.example.ExampleInterface;
import ru.ifmo.ctddev.itegulov.implementor.example.GenericClassExample;

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
}
