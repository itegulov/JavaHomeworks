package ru.ifmo.ctddev.itegulov.implementor;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Class, providing some helpful static methods to work
 * with files
 *
 * @author Daniyar Itegulov
 */
class Utility {

    /**
     * Creates temporary directory
     *
     * @return file, representing temporary directory
     * @throws IOException
     *         if some IO error occurs
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static File createTmpDirectory() throws IOException {
        File file = File.createTempFile("implementor_tmp_", "");
        file.delete();
        file.mkdir();
        return file;
    }

    /**
     * Recursively removes directory
     *
     * @param dir
     *        file, representing directory to remove
     * @throws IOException
     *         if couldn't delete some file
     */
    public static void deleteDirectory(File dir) throws IOException {
        Files.walkFileTree(Paths.get(dir.getPath()), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!file.toFile().delete()) {
                    throw new IOException("Couldn't delete directory");
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (!dir.toFile().delete()) {
                    throw new IOException("Couldn't delete directory");
                }
                return super.postVisitDirectory(dir, exc);
            }

            @Override
            public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
                throw exc;
            }
        });
    }

}