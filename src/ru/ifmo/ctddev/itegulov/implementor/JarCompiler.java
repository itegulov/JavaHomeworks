package ru.ifmo.ctddev.itegulov.implementor;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Allows to compile java sources to jar file
 *
 * @author Daniyar Itegulov
 */
class JarCompiler {

    /**
     * Is being thrown when compiler fails to compile generated implementations' files
     */
    protected static class CompilerException extends Exception {

        public CompilerException(String message) {
            super(message);
        }

    }

    /** Files to compile into jar file */
    private File[] sourceFiles;

    /**
     * Class constructor, specifying what source (.java) files to compile into jar file
     *
     * @param sourceFiles
     *        array of files, which need to be compiled into jar file
     */
    public JarCompiler(final File... sourceFiles) {
        this.sourceFiles = sourceFiles;
    }

    /**
     * Compiles files to <code>jarFile</code>
     *
     * @param jarFile
     *        resulting <tt>.jar</tt> file
     *
     * @throws IOException
     *         if some IO error occurs
     *
     * @throws CompilerException
     *         if compiler fails to compile code
     */
    public void compile(File jarFile) throws IOException, CompilerException {
        File buildDir = null;
        try {
            try {
                buildDir = Utility.mkTmpDir();
            } catch (IOException e) {
                throw new IOException("Can't create temporary directory for class files", e);
            }
            compileClasses(buildDir);
            buildJar(buildDir, jarFile);
        } finally {
            if (buildDir != null && buildDir.exists()) {
                try {
                    Utility.rmDir(buildDir);
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Compiles classes
     *
     * @param buildDir
     *        file, to which we put built classes
     *
     * @throws CompilerException
     *         if compiler fails to compile code
     */
    private void compileClasses(File buildDir) throws CompilerException {
        ArrayList<String> files = new ArrayList<>(sourceFiles.length);
        for (File file : sourceFiles) {
            files.add(file.getPath());
        }
        int exitCode = runCompiler(files, buildDir);
        if (exitCode != 0) {
            throw new CompilerException("Compiler finished with exitCode " + exitCode);
        }
    }

    /**
     * Executes compiler
     *
     * @param files
     *        files to compile
     * @param outDir
     *        where to put built classes
     * @return compiler's exit code
     */
    private int runCompiler(List<String> files, File outDir) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("No java compiler");
        }
        List<String> args = new ArrayList<>();
        args.addAll(files);
        args.add("-d");
        args.add(outDir.getPath());
        return compiler.run(null, null, null, args.toArray(new String[args.size()]));
    }

    /**
     * Builds jar file from built classes
     *
     * @param buildDir
     *        file, representing directory where built files are located
     *
     * @param jarFile
     *        file, representing jar file, which must be created
     *
     * @throws IOException
     *         if failed to find <code>jarFile</code> or failed to
     *         write to jar
     */
    private void buildJar(File buildDir, File jarFile) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try(final JarOutputStream jarOutputStream = new JarOutputStream(
                new BufferedOutputStream(new FileOutputStream(jarFile)), manifest)) {
            final Path buildDirPath = Paths.get(buildDir.getPath());
            Files.walkFileTree(buildDirPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
                    File file = filePath.toFile();
                    String entryName = buildDirPath.relativize(filePath).toString();
                    JarEntry entry = new JarEntry(entryName);
                    entry.setTime(file.lastModified());
                    jarOutputStream.putNextEntry(entry);
                    try(BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
                        byte[] buffer = new byte[65536];
                        while (true) {
                            int count = bis.read(buffer);
                            if (count == -1) {
                                break;
                            }
                            jarOutputStream.write(buffer, 0, count);
                        }
                    }
                    jarOutputStream.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}
