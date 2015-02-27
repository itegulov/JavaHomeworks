package ru.ifmo.ctddev.itegulov.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Takes input file (args[0]), containing list of files and directories to be processed.
 * Then recursively calculates their FVN-hash values and writes result to output file
 * @author Daniyar Itegulov
 * @since 19.02.15
 */
public class RecursiveWalk {
    public static final int INITIAL_HASH = 0x811c9dc5;
    public static final int FVN_PRIME = 0x01000193;
    public static final int BUFFER_SIZE = 2048;

    private static int encode(InputStream inputStream) {
        try (InputStream dataInputStream = new BufferedInputStream(inputStream)) {
            int hash = INITIAL_HASH;
            byte[] bytes = new byte[BUFFER_SIZE];
            int read;
            while ((read = dataInputStream.read(bytes)) != -1) {
                for (int i = 0; i < read; i++) {
                    hash = (hash * FVN_PRIME) ^ (bytes[i] & 0xff);
                }
            }
            return hash;
        } catch (IOException e) {
            return 0;
        }
    }

    private static String convertToHashValue(int hash, Path path) {
        return String.format("%08x %s", hash, path.toString());
    }

    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.err.println("Usage: java RecursiveWalk <input file> <output file>");
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(args[0]), StandardCharsets.UTF_8)) {
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(args[1]), StandardCharsets.UTF_8)) {
                FileVisitor<Path> fileVisitor = new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                        int hash = encode(Files.newInputStream(path));
                        writer.write(convertToHashValue(hash, path));
                        writer.newLine();
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path path, IOException exc) throws IOException {
                        writer.write(convertToHashValue(0, path));
                        writer.newLine();
                        return FileVisitResult.CONTINUE;
                    }
                };
                
                String line;
                while (true) {
                    try {
                        line = reader.readLine();
                        if (line == null) {
                            break;
                        }
                    } catch (IOException e) {
                        System.err.println("Couldn't read from input file");
                        break;
                    }
                    File file = new File(line);
                    Files.walkFileTree(file.toPath(), fileVisitor);
                }
            } catch (IOException e) {
                System.err.println("Couldn't write to output file");
            }
        } catch (IOException e) {
            System.err.println("Couldn't open input file");
        }
    }
}
