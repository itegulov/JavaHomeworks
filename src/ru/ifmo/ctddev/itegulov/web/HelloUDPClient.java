package ru.ifmo.ctddev.itegulov.web;


import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Basic implementation of {@link HelloClient}. Provides way to send simple messages to some server.
 *
 * @author Daniyar Itegulov
 */
public class HelloUDPClient implements HelloClient {
    private static final int TIMEOUT = 200;

    /**
     * Sends UDP-requests to specified host:port, containing {@code "prefix[thread_number]_[requests_number]"},
     * receives responses and print them to stdout. UDP-requests are resent after 200ms timeout or if some I/O error
     * occurs.
     *
     * @param host name or IP-address, representing some server
     * @param port destination port on server
     * @param prefix string, which will be prefix for all sent requests
     * @param requests count of requests to send
     * @param threads number of threads to send requests
     */
    @Override
    public void start(final String host, final int port, final String prefix, final int requests, final int threads) {
        ExecutorService threadPool = Executors.newFixedThreadPool(threads);

        InetAddress address;
        try {
            address = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new IllegalStateException("Unknown host: " + e.getMessage());
        }

        for (int i = 0; i < threads; i++) {
            String threadPrefix = prefix + i + "_";
            threadPool.execute(() -> {
                byte[] sendBuffer;
                byte[] receiveBuffer;

                DatagramSocket socket;
                while (true) { // try until we can open a socket
                    try {
                        socket = new DatagramSocket();
                        socket.setSoTimeout(TIMEOUT);
                        receiveBuffer = new byte[socket.getReceiveBufferSize()];
                        break;
                    } catch (IOException ignore) {
                    }
                }

                for (int j = 0; j < requests; j++) {
                    String request = threadPrefix + j;
                    sendBuffer = request.getBytes(StandardCharsets.UTF_8);

                    while (true) { // try until send completes successfully
                        try {
                            socket.send(new DatagramPacket(sendBuffer, sendBuffer.length, address, port));
                            break;
                        } catch (IOException ignored) {
                        }
                    }

                    try {
                        DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                        socket.receive(packet);
                        String response = new String(packet.getData(), 0, packet.getLength());
                        if (response.equals("Hello, " + request)) {
                            System.out.println(response);
                        } else {
                            j--;
                        }
                    } catch (IOException e) {
                        j--;
                    }
                }
            });
        }

        threadPool.shutdown();
        while (!threadPool.isTerminated());
    }
}