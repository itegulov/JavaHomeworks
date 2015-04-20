package info.kgeorgiy.java.advanced.crawler;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;
import ru.ifmo.ctddev.itegulov.crawler.WebCrawler;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Georgiy Korneev (kgeorgiy@kgeorgiy.info)
 */
@RunWith(JUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CrawlerEasyTest {
    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(final Description description) {
            System.err.println("=== Running " + description.getMethodName());
        }
    };

    @Test
    public void test01_singlePage() throws IOException {
        test("http://en.ifmo.ru/en/page/50/Partnership.htm", 1);
    }

    @Test
    public void test02_pageAndLinks() throws IOException {
        test("http://www.ifmo.ru", 2);
    }

    @Test
    public void test03_deep() throws IOException {
        test("http://www.kgeorgiy.info", 4);
    }

    @Test
    public void test04_noLimits() throws IOException {
        test(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 10, 10);
    }

    @Test
    public void test05_limitDownloads() throws IOException {
        test(10, Integer.MAX_VALUE, Integer.MAX_VALUE, 300, 10);
    }

    @Test
    public void test06_limitExtractors() throws IOException {
        test(Integer.MAX_VALUE, 10, Integer.MAX_VALUE, 10, 300);
    }

    @Test
    public void test07_limitBoth() throws IOException {
        test(10, 10, Integer.MAX_VALUE, 300, 300);
    }

    @Test
    public void test08_performance() throws IOException {
        final long time = test(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 100, 1000);
        System.out.println("Time: " + time);
        Assert.assertTrue("Not parallel", time < 3000);
    }

    private void test(final String url, final int depth) throws IOException {
        test(url, depth, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 10, 10);
    }

    protected long test(final int downloaders, final int extractors, final int perHost, final int downloadTimeout, final int extractTimeout) throws IOException {
        return test("http://neerc.ifmo.ru/subregions/index.html", 3, downloaders, extractors, perHost, downloadTimeout, extractTimeout);
    }

    protected long test(final String url, final int depth, final int downloaders, final int extractors, final int perHost, final int downloadTimeout, final int extractTimeout) throws IOException {
        final long start = System.currentTimeMillis();
        final ReplayDownloader replayDownloader = new ReplayDownloader(URLUtils.getHost(url) + ".ser", downloadTimeout, extractTimeout);
        final Set<String> actual = new HashSet<>(download(url, depth, replayDownloader, downloaders, extractors, perHost));
        final Set<String> missing = diff(replayDownloader.expected(), actual);
        final Set<String> excess = diff(actual, replayDownloader.expected());
        final String message = String.format("\nmissing = %s\nexcess = %s", missing, excess);
        Assert.assertEquals(message, 0, missing.size() + excess.size());
        return System.currentTimeMillis() - start;
    }

    private static List<String> download(final String url, final int depth, final Downloader downloader, final int downloaders, final int extractors, final int perHost) throws IOException {
        final CheckingDownloader checkingDownloader = new CheckingDownloader(downloader, downloaders, extractors, perHost);
        try (Crawler crawler = createInstance(checkingDownloader, downloaders, extractors, perHost)) {
            final List<String> result = crawler.download(url, depth);
            Assert.assertTrue(checkingDownloader.getError(), checkingDownloader.getError() == null);
            return result;
        }
    }

    private static Crawler createInstance(final Downloader downloader, final int downloaders, final int extractors, final int perHost) {
        /*final String className = System.getProperty("cut");
        Assert.assertTrue("Class name not specified", className != null);

        try {
            final Constructor<?> constructor = Class.forName(className).getConstructor(Downloader.class, int.class, int.class, int.class);
            return (Crawler) constructor.newInstance(downloader, downloaders, extractors, perHost);
        } catch (final Exception e) {
            throw new AssertionError(e);
        }*/
        return new WebCrawler(downloader, downloaders, extractors, perHost);
    }

    private static Set<String> diff(final Set<String> a, final Set<String> b) {
        final Set<String> missing = new HashSet<>(a);
        missing.removeAll(b);
        return missing;
    }
}
