package ru.ifmo.ctddev.itegulov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Implementation of interface {@link info.kgeorgiy.java.advanced.implementor.Impler}
 * and {@link info.kgeorgiy.java.advanced.implementor.JarImpler}, with generics supported
 *
 * @author Daniyar Itegulov
 * @see ru.ifmo.ctddev.itegulov.implementor.ImplementHelper
 */
public class Implementor implements Impler, JarImpler {
    /**
     * Implements class, puts it's java code in path, relative to root
     *
     * @param token
     *        class to implement
     *
     * @param root
     *        file, containing root of directory where to put generated file
     *
     * @return file, where source (.java) code was placed
     *
     * @throws ImplerException
     *         if there is no correct implementation for <code>token</code>
     *
     * @see #implement(Class, java.io.File)
     */
    public File implementWithFile(final Class<?> token, final File root) throws ImplerException {
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
        return file;
    }

    @Override
    public void implement(final Class<?> token, final File root) throws ImplerException {
        implementWithFile(token, root);
    }

    @Override
    public void implementJar(final Class<?> token, final File jarFile) throws ImplerException {
        File implDir = null;
        try {
            try {
                implDir = Utility.createTmpDirectory();
            } catch (IOException e) {
                throw new ImplerException("Can't create temporary directory for implementation (source) files", e);
            }
            File file = implementWithFile(token, implDir);
            JarCompiler jarCompiler = new JarCompiler(file);
            try {
                jarCompiler.compile(jarFile);
            } catch (IOException | JarCompiler.CompilerException e) {
                throw new ImplerException(e.getMessage(), e);
            }
        } finally {
            if (implDir != null && implDir.exists()) {
                try {
                    Utility.deleteDirectory(implDir);
                } catch (IOException ignored) {
                }
            }
        }
    }
}
