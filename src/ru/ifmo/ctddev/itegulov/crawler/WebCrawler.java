package ru.ifmo.ctddev.itegulov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Basic implementation of {@link info.kgeorgiy.java.advanced.crawler.Crawler}.
 *
 * @author Daniyar Itegulov
 */
public class WebCrawler implements Crawler {
    private final ExecutorService downloadThreadPool;
    private final ExecutorService extractThreadPool;
    private final Downloader downloader;
    private final ConcurrentHashMap<String, Integer> count = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, BlockingQueue<Worker.DownloaderCallable>> left = new ConcurrentHashMap<>();
    private final int perHost;

    private class Worker {
        private final String url;
        private final int maxDepth;
        private final BlockingQueue<Future<String>> queue = new LinkedBlockingQueue<>();
        private final Set<String> downloaded = Collections.synchronizedSet(new HashSet<>());

        public Worker(String url, int maxDepth) {
            this.url = url;
            this.maxDepth = maxDepth;
        }

        public List<String> process() throws IOException, InterruptedException {
            queue.add(downloadThreadPool.submit(new Worker.DownloaderCallable(url, 1)));
            List<String> result = new ArrayList<>();
            while (!queue.isEmpty()) {
                Future<String> future = queue.poll();
                String res;
                try {
                    res = future.get();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof IOException) {
                        throw (IOException) cause;
                    }

                    if (cause instanceof InterruptedException) {
                        throw (InterruptedException) cause;
                    }

                    if (cause instanceof RuntimeException) {
                        throw (RuntimeException) cause;
                    }

                    if (cause instanceof Error) {
                        throw (Error) cause;
                    }

                    throw new IllegalStateException("Unexpected situation");
                }
                if (res != null) {
                    result.add(res);
                }
            }
            return result;
        }

        private class ExtractorCallable implements Callable<String> {
            private final Document document;
            private final int depth;

            public ExtractorCallable(Document document, int depth) {
                this.document = document;
                this.depth = depth;
            }

            @Override
            public String call() throws IOException, InterruptedException {
                List<String> links;
                links = document.extractLinks();
                for (String link : links) {
                    synchronized (count) {
                        count.putIfAbsent(URLUtils.getHost(link), 0);
                        int cnt = count.get(URLUtils.getHost(link));
                        left.putIfAbsent(URLUtils.getHost(link), new LinkedBlockingQueue<>());
                        if (cnt < perHost) {
                            queue.add(downloadThreadPool.submit(new DownloaderCallable(link, depth + 1)));
                            count.compute(URLUtils.getHost(link), (s, i) -> i + 1);
                        } else {
                            left.get(URLUtils.getHost(link)).put(new DownloaderCallable(link, depth + 1));
                        }
                    }
                }
                //List<Callable<String>> tasks = links.stream().map(
                //        link -> new DownloaderCallable(link, depth + 1)).collect(Collectors.toList());
                //queue.addAll(downloadThreadPool.invokeAll(tasks));
                return null;
            }
        }

        private class DownloaderCallable implements Callable<String> {
            private final String url;
            private final int depth;

            public DownloaderCallable(String url, int depth) {
                this.url = url;
                this.depth = depth;
            }

            @Override
            public String call() throws IOException, InterruptedException {
                synchronized (downloaded) {
                    if (downloaded.contains(url)) {
                        return null;
                    }
                    downloaded.add(url);
                }
                Document document = downloader.download(url);
                if (depth < maxDepth) {
                    queue.put(extractThreadPool.submit(new ExtractorCallable(document, depth)));
                }
                BlockingQueue<DownloaderCallable> q = left.get(URLUtils.getHost(url));
                if (q != null && q.size() > 0) {
                    DownloaderCallable downloaderCallable = q.take();
                    queue.add(downloadThreadPool.submit(downloaderCallable));
                } else {
                    count.compute(URLUtils.getHost(url), (s, integer) -> integer - 1);
                }
                return url;
            }
        }
    }

    /**
     * Class constructor, specifying what {@link Downloader} to use, number of threads,
     * which download, number of threads, which extract and maximal number of threads,
     * which can download from the same host simultaneously ({@code perHost}).
     *
     * @param downloader downloader, which will be used to get web-page
     * @param downloaders number of threads for downloading
     * @param extractors number of threads for extracting links
     * @param perHost maximal number of threads, which can download from the same
     *                host simultaneously
     */
    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.perHost = perHost;
        downloadThreadPool = Executors.newFixedThreadPool(downloaders);
        extractThreadPool = Executors.newFixedThreadPool(extractors);
    }

    /**
     * Gets list of all URLs, that were visited by crawler, starting from {@code url}
     * and lifting by {@code depth} down as most.
     *
     * @param url url, specifying starting position of crawler
     * @param depth maximal depth of web-pages, which will be visited by crawler
     * @return list of all URLs, visited by crawler
     * @throws IOException if an error occurred
     */
    @Override
    public List<String> download(String url, int depth) throws IOException {
        try {
            return new Worker(url, depth).process();
        } catch (InterruptedException e) {
            return null;
        }
    }

    /**
     * Shutdowns all threads, created by crawler. All invocations of {@link
     * #download(String, int)}, that didn't finish yet, will return {@code null}
     * as result.
     */
    @Override
    public void close() {
        downloadThreadPool.shutdown();
        extractThreadPool.shutdown();
        try {
            downloadThreadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            extractThreadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * Main function, which performs crawling of specified url, using specified
     * number of thread for downloading, extracting. Also maximal number of downloaders
     * for the same host can be specified.
     * <p>
     * Usage: WebCrawler url [downloaders [extractors [perHost]]]
     *
     * @param args
     *        array of string arguments, which must match to "Usage"
     */
    public static void main(String[] args) {
        if (args == null || args.length < 1 || args.length > 4) {
            System.err.println("Usage: WebCrawler url [downloaders [extractors [perHost]]]");
            return;
        }
        int downloaders = 1;
        int extractors = 1;
        int perHost = 1;
        if (args.length > 1) {
            downloaders = Integer.parseInt(args[1]);
        }

        if (args.length > 2) {
            extractors = Integer.parseInt(args[2]);
        }

        if (args.length > 3) {
            perHost = Integer.parseInt(args[3]);
        }

        try (WebCrawler webCrawler = new WebCrawler(new CachingDownloader(), downloaders, extractors, perHost)) {
            System.out.println(webCrawler.download(args[0], 3));
        } catch (IOException e) {
            System.err.println("Couldn't download page: " + e.getMessage());
        }
    }
}