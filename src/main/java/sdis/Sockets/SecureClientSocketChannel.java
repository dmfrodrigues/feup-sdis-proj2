package sdis.Sockets;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.net.InetSocketAddress;

public class SecureClientSocketChannel extends SecureSocketChannel {
    private static SSLEngine getSSLEngine(SSLContext sslContext) {
        SSLEngine sslEngine = sslContext.createSSLEngine();
        sslEngine.setUseClientMode(true);
        return sslEngine;
    }

    public SecureClientSocketChannel(InetSocketAddress address, SSLContext sslContext) throws IOException {
        super(address, getSSLEngine(sslContext));
    }
}
