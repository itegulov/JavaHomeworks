package info.kgeorgiy.java.advanced.crawler;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.GZIPInputStream;

/**
 * @author Georgiy Korneev (kgeorgiy@kgeorgiy.info)
 */
public class ReplayDownloader implements Downloader {
    private final ConcurrentMap<String, List<String>> allLinks;
    private final int downloadDelay;
    private final int extractDelay;

    public ReplayDownloader(final String fileName, final int downloadDelay, final int extractDelay) throws IOException {
        allLinks = load(fileName);
        this.downloadDelay = downloadDelay;
        this.extractDelay = extractDelay;
    }

    @SuppressWarnings("unchecked")
    private ConcurrentMap<String, List<String>> load(final String fileName) throws IOException {
        try (ObjectInput os = new ObjectInputStream(new GZIPInputStream(ReplayDownloader.class.getResourceAsStream(fileName)))) {
            try {
                return (ConcurrentMap<String, List<String>>) os.readObject();
            } catch (final ClassNotFoundException e) {
                throw new AssertionError(e);
            }
        }
    }

    @Override
    public Document download(final String url) throws IOException {
        final List<String> links = allLinks.get(url);
        if (links == null) {
            throw new AssertionError("Unknown page " + url);
        }
        System.out.println("Downloading " + url);
        sleep(downloadDelay);
        System.out.println("Downloaded " + url);
        return () -> {
            sleep(extractDelay);
            System.out.println("Links for " + url + ": " + links);
            return links;
        };
    }

    private void sleep(final int max) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(max) + 1);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public Set<String> expected() {
        return allLinks.keySet();
    }
}
