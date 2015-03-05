package ru.ifmo.ctddev.itegulov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import ru.ifmo.ctddev.itegulov.implementor.example.SimpleTemplateInterface;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * @author Daniyar Itegulov
 */
public class Implementor implements Impler {

    public Implementor() {
    }

    public static void main(String[] args) throws ImplerException {
        new Implementor().implement(SimpleTemplateInterface.A.class, new File("src"));
    }

    @Override
    public void implement(final Class<?> token, final File root) throws ImplerException {
        if (token == null || root == null) {
            throw new ImplerException("Null arguments");
        }
        final String path = token.getCanonicalName().replace(".", "/") + "Impl.java";
        File file = new File(root, path).getAbsoluteFile();
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            throw new ImplerException("Couldn't create dirs");
        }
        ImplementHelper implementHelper = new ImplementHelper(token, token.getSimpleName() + "Impl");
        try (PrintWriter printWriter = new PrintWriter(Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8))) {
            try {
                implementHelper.implement(printWriter);
            } catch (IOException e) {
                throw new ImplerException("Couldn't write to output file");
            }
        } catch (IOException e) {
            throw new ImplerException("Couldn't open output file");
        }
    }
}
