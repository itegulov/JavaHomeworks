package info.kgeorgiy.java.advanced.hello;

import info.kgeorgiy.java.advanced.base.BaseTest;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

import static info.kgeorgiy.java.advanced.hello.Util.response;

/**
 * @author Georgiy Korneev (kgeorgiy@kgeorgiy.info)
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HelloServerTest extends BaseTest {
    public static final int PORT = 8888;
    public static final String REQUEST = HelloServerTest.class.getName();

    @Test
    public void test01_singleRequest() throws Throwable {
        test(1, socket -> checkResponse(socket, REQUEST));
    }

    @Test
    public void test02_multipleClients() throws IOException {
        try (HelloServer server = createCUT()) {
            server.start(PORT, 1);
            for (int i = 0; i < 10; i++) {
                client(REQUEST + i);
            }
        }
    }

    @Test
    public void test03_multipleRequests() throws Throwable {
        test(1, socket -> {
            for (int i = 0; i < 10; i++) {
                checkResponse(socket, REQUEST + i);
            }
        });
    }

    @Test
    public void test04_parallelRequests() throws Throwable {
        test(1, socket -> {
            final Set<String> responses = new HashSet<>();
            for (int i = 0; i < 10; i++) {
                final String request = REQUEST + i;
                responses.add(response(request));
                send(socket, request);
            }
            for (int i = 0; i < 10; i++) {
                final String response = Util.receive(socket);
                Assert.assertTrue("Unexpected response " + response, responses.remove(response));
            }
        });
    }

    @Test
    public void test05_parallelClients() throws InterruptedException {
        try (HelloServer server = createCUT()) {
            server.start(PORT, 1);
            parallel(10, () -> client(REQUEST));
        }
    }

    @Test
    public void test06_dos() throws Throwable {
        test(1, socket -> parallel(100, () -> {
            for (int i = 0; i < 10000; i++) {
                send(socket, REQUEST);
            }
        }));
    }

    @Test
    public void test07_noDoS() throws IOException {
        try (HelloServer server = createCUT()) {
            server.start(PORT, 10);
            parallel(10, () -> {
                try (DatagramSocket socket = new DatagramSocket(null)) {
                    for (int i = 0; i < 10000; i++) {
                        checkResponse(socket, REQUEST + i);
                    }
                }
            });
        }
    }

    private void send(final DatagramSocket socket, final String request) throws IOException {
        Util.send(socket, request, new InetSocketAddress("localhost", PORT));
    }

    private void client(final String request) throws IOException {
        try (DatagramSocket socket = new DatagramSocket(null)) {
            checkResponse(socket, request);
        }
    }

    public static void test(final int workers, final ConsumerCommand<DatagramSocket> command) throws Throwable {
        try (HelloServer server = createCUT()) {
            server.start(PORT, workers);
            try (DatagramSocket socket = new DatagramSocket(null)) {
                command.run(socket);
            }
        }
    }

    private void checkResponse(final DatagramSocket socket, final String request) throws IOException {
        final String response = Util.request(request, socket, new InetSocketAddress("localhost", PORT));
        Assert.assertEquals("Invalid response", response(request), response);
    }
}
