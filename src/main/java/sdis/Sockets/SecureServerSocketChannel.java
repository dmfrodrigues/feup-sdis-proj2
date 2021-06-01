package sdis.Sockets;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;

public class SecureServerSocketChannel extends ServerSocketChannel {

    private static SSLEngine getSSLEngine(SSLContext sslContext) {
        SSLEngine sslEngine = sslContext.createSSLEngine();
        sslEngine.setUseClientMode(false);
        return sslEngine;
    }

    private final ServerSocketChannel serverSocketChannel;
    private final SSLContext sslContext;

    public SecureServerSocketChannel(SSLContext sslContext) throws IOException {
        this(ServerSocketChannel.open(), sslContext);
    }

    private SecureServerSocketChannel(ServerSocketChannel serverSocketChannel, SSLContext sslContext) throws IOException {
        super(serverSocketChannel.provider());
        this.serverSocketChannel = serverSocketChannel;
        this.sslContext = sslContext;

        serverSocketChannel.bind(null);
    }

    @Override
    public SocketChannel accept() throws IOException {
        SocketChannel channel = serverSocketChannel.accept();
        if (channel == null) return null;

        channel.configureBlocking(true);

        SSLEngine sslEngine = sslContext.createSSLEngine();
        sslEngine.setUseClientMode(false);

        return new SecureSocketChannel(channel, sslEngine);
    }

    @Override
    public ServerSocketChannel bind(SocketAddress socketAddress, int i) throws IOException {
        return serverSocketChannel.bind(socketAddress, i);
    }

    @Override
    public <T> ServerSocketChannel setOption(SocketOption<T> socketOption, T t) throws IOException {
        return serverSocketChannel.setOption(socketOption, t);
    }

    @Override
    public <T> T getOption(SocketOption<T> socketOption) throws IOException {
        return serverSocketChannel.getOption(socketOption);
    }

    @Override
    public Set<SocketOption<?>> supportedOptions() {
        return serverSocketChannel.supportedOptions();
    }

    @Override
    public ServerSocket socket() {
        return serverSocketChannel.socket();
    }

    @Override
    public SocketAddress getLocalAddress() throws IOException {
        return serverSocketChannel.getLocalAddress();
    }

    @Override
    protected void implCloseSelectableChannel() throws IOException {
        serverSocketChannel.close();
    }

    @Override
    protected void implConfigureBlocking(boolean b) throws IOException {
        serverSocketChannel.configureBlocking(b);
    }
}
