package ru.ifmo.ctddev.itegulov.ui;

import javax.swing.*;
import java.awt.event.WindowEvent;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * @author Daniyar Itegulov
 */
public class UIFileCopy extends JFrame {
    private JProgressBar progressBar;
    private JButton cancelButton;
    private JPanel mainPanel;
    private JPanel infoPanel;
    private JLabel timeSpentLabel;
    private JLabel timeLeftLabel;
    private JLabel averageSpeedLabel;
    private JLabel currentSpeedLabel;

    public UIFileCopy() {
        super("File copy");
        getContentPane().add(mainPanel);
        cancelButton.addActionListener(e -> this.processWindowEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));
    }

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

    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.out.println("Usage: java UIFileCopy <source> <destination>");
            return;
        }
        Path source = Paths.get(args[0]);
        Path destination = Paths.get(args[1]);
        UIFileCopy uiFileCopy = new UIFileCopy();
        uiFileCopy.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        uiFileCopy.pack();
        uiFileCopy.setVisible(true);
        long totalSize;
        try {
            totalSize = FileLengthFetcher.fetch(source);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        uiFileCopy.progressBar.setMaximum(100);
        AtomicLong currentSize = new AtomicLong(0);
        final long startTime = System.nanoTime();

        try {
            Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                    File from = path.toFile();
                    File to = destination.resolve(source.relativize(path)).toFile();
                    try {
                        AtomicLong prevTime = new AtomicLong(System.nanoTime());
                        copyFile(from, to, size -> {
                            long currentTime = System.nanoTime();
                            int percent = (int) Math.round(currentSize.addAndGet(size) * 100.0D / totalSize);
                            uiFileCopy.progressBar.setValue(percent);
                            long elapsed = currentTime - startTime;
                            uiFileCopy.timeSpentLabel.setText(formatTime(elapsed / 1_000_000));
                            double speed = (currentSize.get() * 1_000_000_000.0D / elapsed);
                            uiFileCopy.averageSpeedLabel.setText(formatSpeed((long) speed));
                            double currentSpeed = size * 1_000_000_000.0D / (currentTime - prevTime.get());
                            uiFileCopy.currentSpeedLabel.setText(formatSpeed((long) currentSpeed));
                            long left = totalSize - currentSize.get();
                            long timeLeft = (long) Math.ceil(left / speed);
                            uiFileCopy.timeLeftLabel.setText(formatTime(timeLeft * 1000));
                            prevTime.set(currentTime);
                        });
                    } catch (IOException e) {
                        System.err.println("Pofig na " + to + " potomushto " + e);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    System.out.println("Ochen zhal'");
                    return FileVisitResult.CONTINUE;
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
        } catch (IOException e) {
            e.printStackTrace();
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
                return String.format("%03d mb/s", mBytesPerSecond);
            }
            return String.format("%03d kb/s", kBytesPerSecond);
        }
        return String.format("%03d b/s", bytesPerSecond);
    }

    private static void copyFile(File from, File to, Consumer<Integer> callback) throws IOException {
        byte[] buffer = new byte[16384];
        try (InputStream is = new FileInputStream(from);
             OutputStream os = new FileOutputStream(to)) {
            int length;
            while ((length = is.read(buffer)) >= 0) {
                os.write(buffer, 0, length);
                callback.accept(length);
            }
        }
    }
}
