package info.kgeorgiy.java.advanced.hello;

import org.junit.Assert;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class Util {
    public static final Charset CHARSET = Charset.forName("UTF-8");

    public static String getString(DatagramPacket var0) {
        return new String(var0.getData(), var0.getOffset(), var0.getLength(), CHARSET);
    }

    public static void setString(DatagramPacket var0, String var1) {
        var0.setData(var1.getBytes(CHARSET));
        var0.setLength(var0.getData().length);
    }

    public static DatagramPacket createPacket(DatagramSocket var0) throws SocketException {
        return new DatagramPacket(new byte[var0.getReceiveBufferSize()], var0.getReceiveBufferSize());
    }

    public static String request(String var0, DatagramSocket var1, SocketAddress var2) throws IOException {
        send(var1, var0, var2);
        return receive(var1);
    }

    public static String receive(DatagramSocket var0) throws IOException {
        DatagramPacket var1 = createPacket(var0);
        var0.receive(var1);
        return getString(var1);
    }

    public static void send(DatagramSocket var0, String var1, SocketAddress var2) throws IOException {
        DatagramPacket var3 = new DatagramPacket(new byte[0], 0);
        setString(var3, var1);
        var3.setSocketAddress(var2);
        synchronized(var0) {
            var0.send(var3);
        }
    }

    public static String response(String var0) {
        return "Hello, " + var0;
    }

    public static AtomicInteger[] server(String var0, int var1, double var2, DatagramSocket var4) {
        AtomicInteger[] var5 = Stream.generate(AtomicInteger::new).limit((long)var1).toArray(AtomicInteger[]::new);
        (new Thread(() -> {
            Random var5x = new Random(4357204587045842850L);

            try {
                while(true) {
                    DatagramPacket var6 = createPacket(var4);
                    var4.receive(var6);
                    String var7 = getString(var6);
                    String var8 = "Invalid request " + var7;
                    Assert.assertTrue(var8, var7.startsWith(var0));
                    String[] var9 = var7.substring(var0.length()).split("_");
                    Assert.assertTrue(var8, var9.length == 2);

                    try {
                        int var10 = Integer.parseInt(var9[0]);
                        int var11 = Integer.parseInt(var9[1]);
                        Assert.assertTrue(var8, var11 == var5[var10].get());
                        if(var2 >= var5x.nextDouble()) {
                            var5[var10].incrementAndGet();
                            setString(var6, response(var7));
                            var4.send(var6);
                        }
                    } catch (NumberFormatException var12) {
                        throw new AssertionError(var8);
                    }
                }
            } catch (IOException var13) {
                System.err.println(var13.getMessage());
            }
        })).start();
        return var5;
    }
}