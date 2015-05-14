package ru.ifmo.ctddev.itegulov.web;


import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
     * @param host     name or IP-address, representing some server
     * @param port     destination port on server
     * @param prefix   string, which will be prefix for all sent requests
     * @param requests count of requests to send
     * @param threads  number of threads to send requests
     */
    @Override
    public void start(final String host, final int port, final String prefix, final int requests, final int threads) {
        ExecutorService threadPool = Executors.newFixedThreadPool(threads);
        try {
            InetAddress address;
            try {
                address = InetAddress.getByName(host);
            } catch (UnknownHostException e) {
                throw new IllegalStateException("Unknown host: " + e.getMessage());
            }

            for (int i = 0; i < threads; i++) {
                final int threadId = i;
                threadPool.execute(() -> {
                    try (DatagramSocket socket = new DatagramSocket()) {
                        socket.setSoTimeout(TIMEOUT);
                        byte[] receiveBuffer = new byte[socket.getReceiveBufferSize()];
                        for (int j = 0; j < requests; j++) {
                            String request = prefix + threadId + "_" + j;
                            byte[] sendBuffer = request.getBytes(StandardCharsets.UTF_8);
                            while (true) { // try until send completes successfully
                                try {
                                    socket.send(new DatagramPacket(sendBuffer, sendBuffer.length, address, port));
                                } catch (IOException ignored) {
                                    continue;
                                }
                                break;
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
                    } catch (SocketException e) {
                        throw new IllegalStateException("Socket couldn't be created");
                    }
                });
            }

            threadPool.shutdown();
            threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException ignored) {
        } finally {
            threadPool.shutdownNow();
        }
    }

    private static final String USAGE = "Usage: java HelloUDPClient <hostname> <port> <prefix> <requests> <threads>";

    /**
     * Creates {@link HelloUDPClient} and uses it's {@link #start(String, int, String, int, int)} method with arguments
     * from {@code args}.
     *
     * @param args arguments, which will be passed to created client
     */
    public static void main(String[] args) {
        if (args == null || args.length != 5 || args[0] == null || args[1] == null || args[2] == null
                || args[3] == null || args[4] == null) {
            System.err.println(USAGE);
            return;
        }
        int port, requests, threads;
        try {
            port = Integer.parseInt(args[1]);
            requests = Integer.parseInt(args[3]);
            threads = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            System.err.println(USAGE);
            return;
        }

        try {
            new HelloUDPClient().start(args[0], port, args[2], requests, threads);
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
        }
    }
}