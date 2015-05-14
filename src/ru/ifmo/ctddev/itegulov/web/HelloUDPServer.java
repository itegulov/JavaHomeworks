package ru.ifmo.ctddev.itegulov.web;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Basic implementation of {@link HelloServer}. Provides way to create servers, which will listen up to 65536 ports,
 * receive UDP-packets and resend them back, adding {@code "Hello, "} prefix.
 *
 * @author Daniyar Itegulov
 */
public class HelloUDPServer implements HelloServer {
    private final List<DatagramSocket> sockets = new ArrayList<>();
    private final List<ExecutorService> threadPools = new ArrayList<>();
    private boolean closed = false;

    /**
     * Starts server on specified port, which will receive requests and answer, adding {@code "Hello, } as prefix to
     * them. It will be done simultaneously in specified count of threads.
     *
     * @param port number of port to listen
     * @param threads count of threads to use
     */
    @Override
    public synchronized void start(int port, int threads) {
        if (closed) {
            throw new IllegalStateException("Cannot start, because server is closed");
        }

        DatagramSocket socket;
        DatagramPacket request;

        try {
            socket = new DatagramSocket(port);
            byte[] reqBuf = new byte[socket.getReceiveBufferSize()];
            request = new DatagramPacket(reqBuf, reqBuf.length);
        } catch (SocketException e) {
            throw new IllegalStateException("Couldn't create socket: " + e.getMessage());
        }

        ExecutorService threadPool = Executors.newFixedThreadPool(threads);
        sockets.add(socket);
        threadPools.add(threadPool);
        threadPool.submit(() -> {
            while (!closed) {
                try {
                    socket.receive(request);
                } catch (IOException e) {
                    continue;
                }

                InetAddress clientAddress = request.getAddress();
                int clientPort = request.getPort();
                byte[] respBuf = ("Hello, " + new String(request.getData(), 0, request.getLength()))
                        .getBytes(StandardCharsets.UTF_8);
                DatagramPacket response = new DatagramPacket(respBuf, respBuf.length, clientAddress, clientPort);
                if (threads == 1) {
                    try {
                        socket.send(response);
                    } catch (IOException ignore) {
                    }
                } else {
                    threadPool.submit(() -> {
                        try {
                            socket.send(response);
                        } catch (IOException ignore) {
                        }
                    });
                }
            }
        });
    }


    /**
     * Closes server. {@link #start(int, int)} can't be used after invocation of this method.
     */
    @Override
    public synchronized void close() {
        closed = true;
        sockets.forEach(DatagramSocket::close);
        threadPools.forEach(ExecutorService::shutdownNow);
        sockets.clear();
        threadPools.clear();
    }

    private static final String USAGE = "Usage: java HelloUDPServer <port> <threads>";

    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.err.println(USAGE);
            return;
        }
        int port, threads;
        try {
            port = Integer.parseInt(args[0]);
            threads = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println(USAGE);
            return;
        }

        try (HelloServer server = new HelloUDPServer()) {
            server.start(port, threads);
            synchronized (HelloUDPServer.class) {
                try {
                    HelloUDPServer.class.wait();
                } catch (InterruptedException ignore) {
                }
            }
        }
    }
}