package ru.ifmo.ctddev.itegulov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Implementation of interface Impler, with generics supported
 *
 * @author Daniyar Itegulov
 * @see ru.ifmo.ctddev.itegulov.implementor.ImplementHelper
 */
public class Implementor implements Impler {
    @Override
    public void implement(final Class<?> token, final File root) throws ImplerException {
        if (token == null || root == null) {
            throw new ImplerException("Null arguments");
        }
        final String path = token.getCanonicalName().replace(".", File.separator) + "Impl.java";
        File file = new File(root, path).getAbsoluteFile();
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            throw new ImplerException("Couldn't create dirs");
        }
        try (FileWriter out = new FileWriter(file, true)) {
            try {
                ImplementHelper implementHelper = new ImplementHelper(token, token.getSimpleName() + "Impl", out);
                implementHelper.implement();
            } catch (IOException e) {
                throw new ImplerException("Couldn't write to output file");
            }
        } catch (IOException e) {
            throw new ImplerException("Couldn't open output file");
        }
    }
}
