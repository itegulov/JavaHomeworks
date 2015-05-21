package ru.ifmo.ctddev.itegulov.ui;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Class, providing way to copy files.
 *
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
        private static String[] options = new String[]{"Retry", "Abort", "Skip"};
        private static String[] exists = new String[]{"Replace", "Abort", "Skip"};
        private static String[] visitFailed = new String[]{"Abort", "Skip"};
        byte[] buffer = new byte[16384];
        private Path source;
        private Path destination;
        private UIFileCopyFrame frame;
        private long fileSize = 0;
        private long currentSize = 0;
        private long totalSize = 0;
        private long readSize = 0;
        private long allTime = 0;
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
            Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                    File from = path.toFile();
                    File to = destination.resolve(source.relativize(path)).toFile();
                    if (to.exists()) {
                        int res = JOptionPane.showOptionDialog(
                                frame,
                                String.format("File %s exists", to.toString()),
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
                            if (Thread.currentThread().isInterrupted()) {
                                return FileVisitResult.TERMINATE;
                            }
                            long start = System.nanoTime();
                            try {
                                length = is.read(buffer);
                            } catch (IOException e) {
                                int res = JOptionPane.showOptionDialog(
                                        frame,
                                        String.format("File %s couldn't be read", from.toString()),
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
                                long end = System.nanoTime();
                                allTime += end - start;
                                publish((long) length);
                            } catch (IOException e) {
                                int res = JOptionPane.showOptionDialog(
                                        frame,
                                        String.format("File %s couldn't be written", to.toString()),
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
                            String.format("File %s visit failed", file.toString()),
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
                readSize += size;
                currentSize += size;
                int percent = (int) Math.round(currentSize * 10000.0D / totalSize);
                frame.progressBar.setValue(percent);
                frame.timeSpentLabel.setText(formatTime(allTime / 1_000_000));
                double speed = (currentSize * 1_000_000_000.0D / allTime);
                frame.averageSpeedLabel.setText(formatSpeed((long) speed));
                long left = totalSize - currentSize;
                long timeLeft = (long) Math.ceil(left / speed);
                frame.timeLeftLabel.setText(formatTime(timeLeft * 1000));
                if (currentTime - prevTime >= 100_000_000) {
                    double currentSpeed = readSize * 1_000_000_000.0D / (currentTime - prevTime);
                    frame.currentSpeedLabel.setText(formatSpeed((long) currentSpeed));
                    prevTime = System.nanoTime();
                    readSize = 0;
                }
            }
        }

        @Override
        protected void done() {
            frame.currentSpeedLabel.setText("000 MB/s");
            frame.cancelButton.setEnabled(false);
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
        if (bytesPerSecond >= 1000) {
            long kBytesPerSecond = bytesPerSecond / 1000;
            if (kBytesPerSecond >= 1000) {
                long mBytesPerSecond = kBytesPerSecond / 1000;
                if (mBytesPerSecond >= 1000) {
                    long gBytesPerSecond = mBytesPerSecond / 1000;
                    return String.format("%03d GB/s", gBytesPerSecond);
                }
                return String.format("%03d MB/s", mBytesPerSecond);
            }
            return String.format("%03d KB/s", kBytesPerSecond);
        }
        return String.format("%03d B/s", bytesPerSecond);
    }

    /**
     * Copies first directory/file to second one recursively. Directories are passed through {@code args}.
     * It has GUI with progress bar, average speed, current speed, elapsed time and time remaining.
     *
     * @param args array of strings, which must contain two directory/file paths
     */
    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.out.println("Usage: java UIFileCopyFrame <source> <destination>");
            return;
        }
        Path source = Paths.get(args[0]);
        Path destination = Paths.get(args[1]);
        UIFileCopyFrame frame = new UIFileCopyFrame();
        Copier copier = new Copier(source, destination, frame);
        frame.progressBar.setMaximum(10000);
        frame.cancelButton.addActionListener(e -> {
            copier.cancel(true);
            frame.cancelButton.setEnabled(false);
        });
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(200, 150));
        frame.setSize(300, 300);
        frame.setVisible(true);
        copier.execute();
    }
}
