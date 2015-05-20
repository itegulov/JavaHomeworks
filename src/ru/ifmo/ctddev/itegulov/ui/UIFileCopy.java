package ru.ifmo.ctddev.itegulov.ui;

import javax.swing.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author Daniyar Itegulov
 */
public class UIFileCopy {

    private static class FileLengthFetcher extends SimpleFileVisitor<Path> {
        private long totalSize = 0;

        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
            totalSize += Files.size(path);
            return FileVisitResult.CONTINUE;
        }

        public static long fetch(Path file) throws IOException {
            FileLengthFetcher fileLengthFetcher = new FileLengthFetcher();
            Files.walkFileTree(file, fileLengthFetcher);
            return fileLengthFetcher.totalSize;
        }
    }

    private static class Copier extends SwingWorker<Void, Long> {
        private static String[] options = new String[]{"Retry", "Abort", "Continue"};
        private static String[] exists = new String[]{"Replace", "Abort", "Continue"};
        private static String[] visitFailed = new String[]{"Abort", "Continue"};
        byte[] buffer = new byte[16384];
        private Path source;
        private Path destination;
        private UIFileCopyFrame frame;
        private long fileSize = 0;
        private long currentSize = 0;
        private long totalSize = 0;
        private long startTime;
        private long prevTime;

        public Copier(Path source, Path destination, UIFileCopyFrame frame) {
            super();
            this.source = source;
            this.destination = destination;
            this.frame = frame;
        }

        @Override
        protected Void doInBackground() throws IOException {
            totalSize = FileLengthFetcher.fetch(source);
            startTime = System.nanoTime();
            Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                    File from = path.toFile();
                    File to = destination.resolve(source.relativize(path)).toFile();
                    if (to.exists()) {
                        int res = JOptionPane.showOptionDialog(
                                frame,
                                String.format("File %s exists", to),
                                "File exists",
                                JOptionPane.YES_NO_CANCEL_OPTION,
                                JOptionPane.WARNING_MESSAGE,
                                null,
                                exists,
                                null);
                        if (res == 1) {
                            return FileVisitResult.TERMINATE;
                        } else if (res == 2) {
                            publish(from.length());
                            return FileVisitResult.CONTINUE;
                        }
                    }
                    prevTime = System.nanoTime();
                    try (InputStream is = new FileInputStream(from);
                         OutputStream os = new FileOutputStream(to)) {
                        int length;
                        while (true) {
                            try {
                                length = is.read(buffer);
                            } catch (IOException e) {
                                int res = JOptionPane.showOptionDialog(
                                        frame,
                                        String.format("File %s couldn't be read", from),
                                        "File read error",
                                        JOptionPane.YES_NO_CANCEL_OPTION,
                                        JOptionPane.WARNING_MESSAGE,
                                        null,
                                        options,
                                        null);
                                if (res == 0) {
                                    continue;
                                } else if (res == 1) {
                                    return FileVisitResult.TERMINATE;
                                } else if (res == 2) {
                                    publish(from.length() - fileSize);
                                    return FileVisitResult.CONTINUE;
                                }
                                break;
                            }
                            if (length < 0) {
                                break;
                            }
                            try {
                                os.write(buffer, 0, length);
                                publish((long) length);
                            } catch (IOException e) {
                                int res = JOptionPane.showOptionDialog(
                                        frame,
                                        String.format("File %s couldn't be written", to),
                                        "File write error",
                                        JOptionPane.YES_NO_CANCEL_OPTION,
                                        JOptionPane.WARNING_MESSAGE,
                                        null,
                                        options,
                                        null);
                                if (res == 0) {
                                    continue;
                                } else if (res == 1) {
                                    return FileVisitResult.TERMINATE;
                                } else if (res == 2) {
                                    publish(from.length() - fileSize);
                                    return FileVisitResult.CONTINUE;
                                }
                                break;
                            }
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    int res = JOptionPane.showOptionDialog(
                            frame,
                            String.format("File %s visit failed", file),
                            "File visit failed",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.WARNING_MESSAGE,
                            null,
                            visitFailed,
                            null);
                    if (res == 0) {
                        return FileVisitResult.TERMINATE;
                    } else {
                        return FileVisitResult.CONTINUE;
                    }
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    try {
                        Files.createDirectory(destination.resolve(source.relativize(dir)));
                    } catch (FileAlreadyExistsException ignored) {
                    }
                    return super.preVisitDirectory(dir, attrs);
                }
            });
            return null;
        }

        @Override
        protected void process(List<Long> chunks) {
            for (long size : chunks) {
                long currentTime = System.nanoTime();
                fileSize += size;
                currentSize += size;
                int percent = (int) Math.round(currentSize * 10000.0D / totalSize);
                frame.progressBar.setValue(percent);
                long elapsed = currentTime - startTime;
                frame.timeSpentLabel.setText(formatTime(elapsed / 1_000_000));
                double speed = (currentSize * 1_000_000_000.0D / elapsed);
                frame.averageSpeedLabel.setText(formatSpeed((long) speed));
                double currentSpeed = size * 1_000_000_000.0D / (currentTime - prevTime);
                frame.currentSpeedLabel.setText(formatSpeed((long) currentSpeed));
                long left = totalSize - currentSize;
                long timeLeft = (long) Math.ceil(left / speed);
                frame.timeLeftLabel.setText(formatTime(timeLeft * 1000));
                prevTime = currentTime;
            }
        }
    }

    private static String formatTime(long l) {
        final long hr = TimeUnit.MILLISECONDS.toHours(l);
        final long min = TimeUnit.MILLISECONDS.toMinutes(l -
                TimeUnit.HOURS.toMillis(hr));
        final long sec = TimeUnit.MILLISECONDS.toSeconds(l -
                TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min));
        return String.format("%02d:%02d:%02d", hr, min, sec);
    }

    private static String formatSpeed(long bytesPerSecond) {
        if (bytesPerSecond >= 1024) {
            long kBytesPerSecond = bytesPerSecond / 1024;
            if (kBytesPerSecond >= 1024) {
                long mBytesPerSecond = kBytesPerSecond / 1024;
                return String.format("%03d MiB/s", mBytesPerSecond);
            }
            return String.format("%03d KiB/s", kBytesPerSecond);
        }
        return String.format("%03d B/s", bytesPerSecond);
    }

    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.out.println("Usage: java UIFileCopyFrame <source> <destination>");
            return;
        }
        Path source = Paths.get(args[0]);
        Path destination = Paths.get(args[1]);
        UIFileCopyFrame frame = new UIFileCopyFrame();
        frame.progressBar.setMaximum(10000);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(300, 300);
        frame.setVisible(true);
        Copier copier = new Copier(source, destination, frame);
        copier.execute();
    }
}
