package sdis.Sockets;

import org.junit.Test;
import sdis.Utils.Utils;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;

import static org.junit.Assert.assertArrayEquals;

public class TestSockets {

    public TestSockets() {
    }

    private SSLContext getSSLContext() throws GeneralSecurityException, IOException {
        // Create and initialize the SSLContext with key material
        char[] password = Files.readString(Path.of("keys/password")).toCharArray();

        // First initialize the key and trust material
        KeyStore ksKeys = KeyStore.getInstance("JKS");
        ksKeys.load(new FileInputStream("keys/client"), password);
        KeyStore ksTrust = KeyStore.getInstance("JKS");
        ksTrust.load(new FileInputStream("keys/truststore"), password);

        // KeyManagers decide which key material to use
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("PKIX");
        kmf.init(ksKeys, password);

        // TrustManagers decide whether to allow connections
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
        tmf.init(ksTrust);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());

        return sslContext;

        /*
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(Utils.getKeyManagers("keys/client", "123456"), Utils.getTrustManagers("keys/truststore", "123456"), new SecureRandom());
        return context;
        */
    }

    @Test(timeout=10000)
    public void test1() throws Exception {
        final SSLContext sslContext = getSSLContext();

        InetAddress ipAddress = InetAddress.getByName("localhost");
        ServerSocketChannel serverSocketChannel = new SecureServerSocketChannel(sslContext);
        InetSocketAddress socketAddress = new InetSocketAddress(ipAddress, serverSocketChannel.socket().getLocalPort());

        Thread thread = new Thread(() -> {
            try {
                SocketChannel socketChannel = Utils.createSocket(sslContext, socketAddress);
                ByteBuffer outBuffer = ByteBuffer.wrap("Hello World!".getBytes());
                socketChannel.write(outBuffer);

                socketChannel.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();

        SocketChannel socketChannel = serverSocketChannel.accept();
        ByteBuffer inBuffer = ByteBuffer.allocate(100);
        socketChannel.read(inBuffer);
        inBuffer.flip();
        byte[] array = new byte[inBuffer.limit()];
        System.arraycopy(inBuffer.array(), inBuffer.position(), array, 0, array.length);
        assertArrayEquals("Hello World!".getBytes(), array);
        System.out.println(new String(array));

        socketChannel.close();

        thread.join();
    }

    @Test(timeout=10000)
    public void test1_1() throws Exception {
        final SSLContext sslContext = getSSLContext();

        InetAddress ipAddress = InetAddress.getByName("localhost");
        ServerSocketChannel serverSocketChannel = new SecureServerSocketChannel(sslContext);
        InetSocketAddress socketAddress = new InetSocketAddress(ipAddress, serverSocketChannel.socket().getLocalPort());

        Thread thread = new Thread(() -> {
            try {
                SocketChannel socketChannel = Utils.createSocket(sslContext, socketAddress);
                ByteBuffer outBuffer = ByteBuffer.wrap("Hello World!".getBytes());
                socketChannel.write(outBuffer);

                ByteBuffer inBuffer = ByteBuffer.allocate(100);
                socketChannel.read(inBuffer);
                inBuffer.flip();
                byte[] array = new byte[inBuffer.limit()];
                System.arraycopy(inBuffer.array(), inBuffer.position(), array, 0, array.length);
                assertArrayEquals("Hello back!".getBytes(), array);
                System.out.println(new String(array));

                socketChannel.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();

        SocketChannel socketChannel = serverSocketChannel.accept();
        ByteBuffer inBuffer = ByteBuffer.allocate(100);
        socketChannel.read(inBuffer);
        inBuffer.flip();
        byte[] array = new byte[inBuffer.limit()];
        System.arraycopy(inBuffer.array(), inBuffer.position(), array, 0, array.length);
        assertArrayEquals("Hello World!".getBytes(), array);
        System.out.println(new String(array));

        ByteBuffer outBuffer = ByteBuffer.wrap("Hello back!".getBytes());
        socketChannel.write(outBuffer);

        socketChannel.close();

        thread.join();
    }

    @Test(timeout=10000)
    public void test2() throws Exception {
        final SSLContext sslContext = getSSLContext();

        byte[] data = new byte[30000];
        for(int i = 0; i < data.length; ++i){
            data[i] = Integer.toString(i % 10).getBytes()[0];
        }

        InetAddress ipAddress = InetAddress.getByName("localhost");
        ServerSocketChannel serverSocketChannel = new SecureServerSocketChannel(sslContext);
        InetSocketAddress socketAddress = new InetSocketAddress(ipAddress, serverSocketChannel.socket().getLocalPort());

        Thread thread = new Thread(() -> {
            try {
                SocketChannel socketChannel = Utils.createSocket(sslContext, socketAddress);
                ByteBuffer buffer = ByteBuffer.wrap(data);
                socketChannel.write(buffer);

                socketChannel.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();

        SocketChannel socketChannel = serverSocketChannel.accept();
        ByteBuffer buffer = ByteBuffer.allocate(31000);
        socketChannel.read(buffer);
        buffer.flip();
        byte[] array = new byte[buffer.limit()];
        System.arraycopy(buffer.array(), buffer.position(), array, 0, array.length);
        assertArrayEquals(data, array);

        socketChannel.close();

        thread.join();
    }

    @Test(timeout=1000)
    public void test3() throws Exception {
        final SSLContext sslContext = getSSLContext();
        byte[] data = new byte[46210];
        for(int i = 0; i < data.length; ++i){
            data[i] = Integer.toString(i % 10).getBytes()[0];
        }

        InetAddress ipAddress = InetAddress.getByName("localhost");
        ServerSocketChannel serverSocketChannel = new SecureServerSocketChannel(sslContext);
        InetSocketAddress socketAddress = new InetSocketAddress(ipAddress, serverSocketChannel.socket().getLocalPort());

        Thread thread = new Thread(() -> {
            try {
                SocketChannel socketChannel = Utils.createSocket(sslContext, socketAddress);
                ByteBuffer buffer = ByteBuffer.wrap(data);
                socketChannel.write(buffer);

                socketChannel.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();

        SocketChannel socketChannel = serverSocketChannel.accept();
        ByteBuffer buffer = ByteBuffer.allocate(47000);
        socketChannel.read(buffer);
        buffer.flip();
        byte[] array = new byte[buffer.limit()];
        System.arraycopy(buffer.array(), buffer.position(), array, 0, array.length);
        assertArrayEquals(data, array);

        socketChannel.close();

        thread.join();
    }

    @Test(timeout=1000)
    public void test4() throws Exception {
        final SSLContext sslContext = getSSLContext();

        byte[] data = new byte[70000];
        for(int i = 0; i < data.length; ++i){
            data[i] = Integer.toString(i % 10).getBytes()[0];
        }

        InetAddress ipAddress = InetAddress.getByName("localhost");
        ServerSocketChannel serverSocketChannel = new SecureServerSocketChannel(sslContext);
        InetSocketAddress socketAddress = new InetSocketAddress(ipAddress, serverSocketChannel.socket().getLocalPort());

        Thread thread = new Thread(() -> {
            try {
                SocketChannel socketChannel = Utils.createSocket(sslContext, socketAddress);
                ByteBuffer buffer = ByteBuffer.wrap(data);
                socketChannel.write(buffer);

                socketChannel.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();

        SocketChannel socketChannel = serverSocketChannel.accept();
        ByteBuffer buffer = ByteBuffer.allocate(70010);
        socketChannel.read(buffer);
        buffer.flip();
        byte[] array = new byte[buffer.limit()];
        System.arraycopy(buffer.array(), buffer.position(), array, 0, array.length);
        assertArrayEquals(data, array);

        socketChannel.close();

        thread.join();
    }
}
