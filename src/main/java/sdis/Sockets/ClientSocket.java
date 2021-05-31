package sdis.Sockets;

import sdis.Modules.Main.Messages.MainMessage;
import sdis.Modules.Message;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.*;
import java.security.cert.CertificateException;

public class ClientSocket extends SSLsocket{

    private SSLEngine engine;
    private SocketChannel socketChannel;

    private void init(InetSocketAddress address) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException  {
        SSLContext sslContext;

        // Create and initialize the SSLContext with key material
        char[] passphrase = "123456".toCharArray();

        // First initialize the key and trust material
        KeyStore ksKeys = KeyStore.getInstance("JKS");
        ksKeys.load(new FileInputStream("keys/client"), passphrase);
        KeyStore ksTrust = KeyStore.getInstance("JKS");
        ksTrust.load(new FileInputStream("keys/truststore"), passphrase);
        // KeyManagers decide which key material to use
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("PKIX");
        kmf.init(ksKeys, passphrase);
        // TrustManagers decide whether to allow connections
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
        tmf.init(ksTrust);

        sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
        engine = sslContext.createSSLEngine(address.getAddress().getHostName(), address.getPort());
        engine.setUseClientMode(true);

        SSLSession session = engine.getSession();
        appData = ByteBuffer.allocate(64000);
        netData = ByteBuffer.allocate(session.getPacketBufferSize());
        peerAppData = ByteBuffer.allocate(64000);
        peerNetData = ByteBuffer.allocate(session.getPacketBufferSize());
    }

    private void write(Message m) throws IOException {
        appData.clear();
        appData.put(m.asByteArray());
        appData.flip();
        while(appData.hasRemaining()){
            netData.clear();
            SSLEngineResult res = engine.wrap(appData, netData);
            switch (res.getStatus()){
                case OK:
                    // Send Encoded Data
                    while(netData.hasRemaining()){
                        int num = socketChannel.write(netData);
                    }
                break;
                case BUFFER_OVERFLOW:
                    // Expand buffer
                    break;
                case CLOSED:
                    // Close connection
                    break;
                default:
                    break;
            }
        }
    }

    public void read() throws IOException {
        peerNetData.clear();
        int bytes = socketChannel.read(peerNetData);
        if(bytes > 0) {
            while (peerNetData.hasRemaining()) {
                SSLEngineResult res = engine.unwrap(peerNetData, peerAppData);
                // Process status of call
                switch (res.getStatus()) {
                    case OK:
                        peerAppData.flip();
                        break;
                    case BUFFER_OVERFLOW:
                        if (engine.getSession().getApplicationBufferSize() > peerAppData.capacity()) {
                            peerAppData = ByteBuffer.allocate(engine.getSession().getApplicationBufferSize());
                        } else {
                            peerAppData = ByteBuffer.allocate(peerAppData.capacity() * 2);
                        }
                        break;

                    case BUFFER_UNDERFLOW:
                        if (engine.getSession().getPacketBufferSize() > peerNetData.capacity()) {
                            peerNetData = ByteBuffer.allocate(engine.getSession().getPacketBufferSize());
                        } else {
                            peerNetData.compact();
                        }
                        break;
                    case CLOSED:
                        // Close connection
                        engine.closeOutbound();
                        handshake(socketChannel, engine);
                        socketChannel.close();
                        break;
                    default:
                        break;
                }
            }
        }else if(bytes < 0){
            engine.closeInbound();
        }
    }

    public SocketChannel send(InetSocketAddress to, Message m) throws IOException, UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        init(to);

        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(to);

        engine.beginHandshake();

        // handshake();
        if(handshake(socketChannel, engine)){
            //Send message
            write(m);
        }
        else // handshake failed
            return null;

        return null;
    }
}
