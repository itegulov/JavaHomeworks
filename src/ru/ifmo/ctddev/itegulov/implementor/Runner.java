package ru.ifmo.ctddev.itegulov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.File;

/**
 * Default runner for {@link ru.ifmo.ctddev.itegulov.implementor.Implementor}
 * <p>
 * Usage:
 * <p>
 * Syntax: java [-jar] className [jarFile|outDir]
 * <p>
 * Allows to generate .java file and .jar (by specifying -jar argument) files.
 * @author Daniyar Itegulov
 */
public class Runner {

    /**
     * Main function, which performs exactly the same thing as
     * {@link ru.ifmo.ctddev.itegulov.implementor.Implementor#implement} or
     * {@link ru.ifmo.ctddev.itegulov.implementor.Implementor#implementJar}
     * (depends on -jar argument) using <code>args</code> as arguments.
     *
     * @param args
     *        array of string arguments, which can contain -jar and
     *        arguments for <code>Implementor</code>
     */
    public static void main(String[] args) {
        if (args == null || args.length < 2 || args[0] == null || args[1] == null) {
            throw new IllegalArgumentException("Invalid usage");
        }
        if (args[0].equals("-jar")) {
            if (args.length != 3 || args[2] == null) {
                throw new IllegalArgumentException("Invalid usage");
            }
            try {
                new Implementor().implementJar(Class.forName(args[1]), new File(args[2]));
            } catch (ClassNotFoundException e) {
                System.err.println("Can't find class " + args[0]);
            } catch (ImplerException e) {
                System.err.println("Can't implement class " + args[0] + ": " + e.getMessage());
            }
        } else {
            try {
                new Implementor().implement(Class.forName(args[0]), new File(args[1]));
            } catch (ClassNotFoundException e) {
                System.err.println("Can't find class " + args[0]);
            } catch (ImplerException e) {
                System.err.println("Can't implement class " + args[0] + ": " + e.getMessage());
            }
        }
    }
}
