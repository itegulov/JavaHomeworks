package info.kgeorgiy.java.advanced.crawler;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;

import java.io.IOException;

/**
 * @author Georgiy Korneev (kgeorgiy@kgeorgiy.info)
 */
@RunWith(JUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CrawlerHardTest extends CrawlerEasyTest {
    @Test
    public void test09_singleConnectionPerHost() throws IOException {
        test("http://www.ifmo.ru", 2, Integer.MAX_VALUE, Integer.MAX_VALUE, 1, 10, 10);
    }

    @Test
    public void test09_limitedConnectionsPerHost() throws IOException {
        test("http://www.ifmo.ru", 2, Integer.MAX_VALUE, Integer.MAX_VALUE, 10, 10, 10);
    }
}
