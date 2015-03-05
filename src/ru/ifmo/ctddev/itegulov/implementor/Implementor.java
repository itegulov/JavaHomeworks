package ru.ifmo.ctddev.itegulov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import ru.ifmo.ctddev.itegulov.implementor.example.SimpleClassExtended;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Daniyar Itegulov
 * @since 27.02.15
 */
public class Implementor implements Impler {

    public Implementor() {
    }

    @Override
    public void implement(final Class<?> token, final File root) throws ImplerException {
        if (token == null || root == null) {
            throw new ImplerException("Null arguments");
        }
        String packagePath = token.getPackage() != null ? token.getPackage().getName().replace(".", File.separator) : "";
        String subPath = token.getSimpleName() + "Impl.java";
        Path resultPath = root.toPath().resolve(packagePath).resolve(subPath);
        if (!resultPath.getParent().toFile().exists() && !resultPath.getParent().toFile().mkdirs()) {
            throw new ImplerException("Couldn't create dirs");
        }
        ImplementHelper implementHelper = new ImplementHelper(token, token.getSimpleName() + "Impl");
        try (PrintWriter printWriter = new PrintWriter(Files.newBufferedWriter(resultPath, StandardCharsets.UTF_8))) {
            try {
                implementHelper.implement(printWriter);
            } catch (IOException e) {
                throw new ImplerException("Couldn't write to output file");
            }
        } catch (IOException e) {
            throw new ImplerException("Couldn't open output file");
        }
    }

    public static void main(String[] args) throws ImplerException {
        new Implementor().implement(SimpleClassExtended.class, new File("src"));
    }
}
