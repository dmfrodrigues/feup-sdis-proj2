package sdis.Sockets;

import sdis.Modules.Main.Main;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;

public class SecureSocketChannel extends SocketChannel {

    private final SSLEngine engine;
    private final SocketChannel socketChannel;

    private ByteBuffer appData     = ByteBuffer.allocate(Main.CHUNK_SIZE + Main.MAX_HEADER_SIZE);   /** @brief Plaintext data **/
    private ByteBuffer netData;                                                                     /** @brief Encrypted data **/
    private ByteBuffer peerAppData = ByteBuffer.allocate(Main.CHUNK_SIZE + Main.MAX_HEADER_SIZE);
    private ByteBuffer peerNetData;

    public SecureSocketChannel(InetSocketAddress address, SSLEngine engine) throws IOException {
        this(SocketChannel.open(), address, engine);
    }

    public SecureSocketChannel(SocketChannel socketChannel, SSLEngine engine) throws IOException {
        super(socketChannel.provider());
        this.engine = engine;
        this.socketChannel = socketChannel;
        initializeNetBuffers();
        this.engine.beginHandshake();
    }

    private SecureSocketChannel(SocketChannel socketChannel, InetSocketAddress socketAddress, SSLEngine engine) throws IOException {
        super(socketChannel.provider());
        this.engine = engine;
        this.socketChannel = socketChannel;
        initializeNetBuffers();
        this.engine.beginHandshake();
        this.socketChannel.connect(socketAddress);
    }

    private void initializeNetBuffers() {
        SSLSession session = engine.getSession();
        netData = ByteBuffer.allocate(session.getPacketBufferSize());
        peerNetData = ByteBuffer.allocate(session.getPacketBufferSize());
    }

    private boolean handshake(boolean b) throws IOException {
        netData.clear();
        peerNetData.clear();

        // Begin handshake
        engine.beginHandshake();
        SSLEngineResult.HandshakeStatus hs = engine.getHandshakeStatus();

        // Process handshaking message
        while (hs != SSLEngineResult.HandshakeStatus.FINISHED &&
                hs != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {

            switch (hs) {
                case NEED_UNWRAP:
                    // Receive handshaking data from peer
                    if (socketChannel.read(peerNetData) < 0) {
                        // The channel has reached end-of-stream
                        if(engine.isOutboundDone() && engine.isInboundDone())
                            return false;
                        engine.closeInbound();
                        engine.closeOutbound();
                        break;
                    }
                    // Process incoming handshaking data
                    peerNetData.flip(); // will set the buffer limit to the current position and reset the position to zero
                    SSLEngineResult res = engine.unwrap(peerNetData, peerAppData);
                    peerNetData.compact();
                    hs = res.getHandshakeStatus();

                    // Check status
                    switch (res.getStatus()) {
                        case OK :
                            break;
                        case BUFFER_OVERFLOW:
                            // Maybe need to enlarge the peer application data buffer if
                            // it is too small, and be sure you've compacted/cleared the
                            // buffer from any previous operations.
                            if (engine.getSession().getApplicationBufferSize() > peerAppData.capacity()) {
                                // enlarge the peer application data buffer
                                peerAppData = ByteBuffer.allocate(engine.getSession().getApplicationBufferSize());
                            } else {
                                // compact or clear the buffer
                                peerAppData = ByteBuffer.allocate(peerAppData.capacity() * 2);
                            }
                            // retry the operation ?
                            break;

                        case BUFFER_UNDERFLOW:
                            // Not enough inbound data to process. Obtain more network data
                            // and retry the operation. You may need to enlarge the peer
                            // network packet buffer, and be sure you've compacted/cleared
                            // the buffer from any previous operations.
                            if (engine.getSession().getPacketBufferSize() > peerNetData.capacity()) {
                                // enlarge the peer network packet buffer
                                peerNetData = ByteBuffer.allocate(engine.getSession().getPacketBufferSize());
                            } else {
                                // compact or clear the buffer
                                peerNetData.compact();
                            }
                            // obtain more inbound network data and then retry the operation
                            break;
                        case CLOSED:
                            // Close connection
                            if (engine.isOutboundDone()) {
                                return false;
                            }
                            engine.closeOutbound();
                            socketChannel.close();
                            break;
                        default:
                            break;
                    }
                    break;
                case NEED_WRAP:
                    // Ensure that any previous net data in myNetData has been sent

                    // Empty/clear the local network packet buffer.
                    netData.clear();

                    // Generate more data to send if possible.
                    res = engine.wrap(appData, netData);
                    hs = res.getHandshakeStatus();
                    // Check status
                    switch (res.getStatus()) {
                        case OK :
                            netData.flip();
                            // Send the handshaking data to peer
                            while (netData.hasRemaining()) {
                                socketChannel.write(netData);
                            }
                            break;
                        case BUFFER_OVERFLOW:
                            if (engine.getSession().getApplicationBufferSize() > peerAppData.capacity()) {
                                // enlarge the peer application data buffer
                                peerAppData = ByteBuffer.allocate(engine.getSession().getApplicationBufferSize());
                            } else {
                                // compact or clear the buffer
                                peerAppData = ByteBuffer.allocate(peerAppData.capacity() * 2);
                            }
                            break;
                        case BUFFER_UNDERFLOW:
                            return false;
                        case CLOSED:
                            // Close connection
                            netData.flip();
                            while (netData.hasRemaining()) {
                                socketChannel.write(netData);
                            }
                            peerNetData.clear();
                            break;
                        default:
                            break;
                    }
                    break;
                case NEED_TASK :
                    // Handle blocking tasks
                    Runnable task;
                    while ((task = engine.getDelegatedTask()) != null) {
                        new Thread(task).start();
                    }
                    break;
                default: // FINISHED or NOT_HANDSHAKING
                    break;
            }
            hs = engine.getHandshakeStatus();
        }
        return true;
    }

    private int doWrite(ByteBuffer src) throws IOException {
        if(!handshake(false)) return 0;

        appData.clear();

        // Copy app data from src to appData
        if(src.remaining() > appData.remaining()){
            appData = ByteBuffer.allocate(src.remaining() * 2);
        }
        appData.put(src);
        appData.flip();

        // Process app data
        while(appData.hasRemaining()){
            netData.clear();
            SSLEngineResult res = engine.wrap(appData, netData);
            switch (res.getStatus()){
                case OK:
                    int ret = 0;
                    while(netData.hasRemaining()){
                        ret += socketChannel.write(netData);
                    }
                    return ret;
                case BUFFER_OVERFLOW:
                    netData = ByteBuffer.allocate(netData.capacity() * 2);
                    break;
                case CLOSED: // Close connection
                    engine.closeOutbound();
                    handshake(false);
                    socketChannel.close();
                    throw new ClosedChannelException();
                default:
                    break;
            }
        }

        return 0;
    }

    public int doRead(ByteBuffer peerAppData) throws IOException {
        if(!handshake(true)) return 0;

        peerNetData.clear();
        int bytes = socketChannel.read(peerNetData);

        if(bytes > 0) {
            while (peerNetData.hasRemaining()) {
                SSLEngineResult res = engine.unwrap(peerNetData, peerAppData);
                // Process status of call
                switch (res.getStatus()) {
                    case OK:
                        return bytes;
                    case BUFFER_OVERFLOW:
                        throw new BufferOverflowException();
                    case BUFFER_UNDERFLOW:
                        if (engine.getSession().getPacketBufferSize() > peerNetData.capacity()) {
                            peerNetData = ByteBuffer.allocate(engine.getSession().getPacketBufferSize());
                        } else {
                            peerNetData.compact();
                        }
                        break;
                    case CLOSED: // Close connection
                        engine.closeOutbound();
                        handshake(true);
                        socketChannel.close();
                        break;
                    default:
                        break;
                }
            }
        } else if(bytes < 0) {
            engine.closeInbound();
        }

        return bytes;
    }

    /** Read/write methods **/

    @Override
    public int read(ByteBuffer byteBuffer) throws IOException {
        return doRead(byteBuffer);
    }

    @Override
    public long read(ByteBuffer[] byteBuffers, int i, int i1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int write(ByteBuffer byteBuffer) throws IOException {
        return doWrite(byteBuffer);
    }

    @Override
    public long write(ByteBuffer[] byteBuffers, int i, int i1) throws IOException {
        throw new UnsupportedOperationException();
    }

    /** Protected calls; redirect to socketChannel but with some more stuff **/

    @Override
    protected void implCloseSelectableChannel() throws IOException {
        ByteBuffer empty = ByteBuffer.allocate(0);

        // Call and process closeInbound
        engine.closeInbound();
        while (!engine.isInboundDone()) {
            SSLEngineResult res = engine.wrap(empty, netData);  // Get close message
            switch (res.getStatus()) {
                case OK: // Send close message to peer
                    while(netData.hasRemaining()) {
                        socketChannel.write(netData);
                        netData.compact();
                    }
                    break;
                case CLOSED: break;
                case BUFFER_OVERFLOW: netData = ByteBuffer.allocate(netData.capacity() * 2); break;
                default: break;
            }
        }

        // Call and process closeOutbound
        engine.closeOutbound();
        while (!engine.isOutboundDone()) {
            SSLEngineResult res = engine.wrap(empty, netData);  // Get close message
            switch (res.getStatus()) {
                case OK: // Send close message to peer
                    while(netData.hasRemaining()) {
                        socketChannel.write(netData);
                        netData.compact();
                    }
                    break;
                case CLOSED: break;
                case BUFFER_OVERFLOW: netData = ByteBuffer.allocate(netData.capacity() * 2); break;
                default: break;
            }
        }

        // Close medium
        socketChannel.close();
    }

    @Override
    protected void implConfigureBlocking(boolean b) throws IOException {
        socketChannel.configureBlocking(b);
    }

    /** Shutdown input/output */

    @Override
    public SocketChannel shutdownInput() throws IOException {
        return socketChannel.shutdownInput();
    }

    @Override
    public SocketChannel shutdownOutput() throws IOException {
        ByteBuffer empty = ByteBuffer.allocate(0);
        engine.closeOutbound();
        while (!engine.isOutboundDone()) {
            SSLEngineResult res = engine.wrap(empty, netData);  // Get close message
            switch (res.getStatus()) {
                case OK: // Send close message to peer
                    while(netData.hasRemaining()) {
                        socketChannel.write(netData);
                        netData.compact();
                    }
                    break;
                case CLOSED: break;
                case BUFFER_OVERFLOW: netData = ByteBuffer.allocate(netData.capacity() * 2); break;
                default: break;
            }
        }

        return socketChannel.shutdownOutput();
    }

    /** Just redirect methods to socketChannel */

    @Override
    public SocketChannel bind(SocketAddress socketAddress) throws IOException {
        return socketChannel.bind(socketAddress);
    }

    @Override
    public <T> SocketChannel setOption(SocketOption<T> socketOption, T t) throws IOException {
        return socketChannel.setOption(socketOption, t);
    }

    @Override
    public <T> T getOption(SocketOption<T> socketOption) throws IOException {
        return socketChannel.getOption(socketOption);
    }

    @Override
    public Set<SocketOption<?>> supportedOptions() {
        return socketChannel.supportedOptions();
    }

    @Override
    public Socket socket() {
        return socketChannel.socket();
    }

    @Override
    public boolean isConnected() {
        return socketChannel.isConnected();
    }

    @Override
    public boolean isConnectionPending() {
        return socketChannel.isConnectionPending();
    }

    @Override
    public boolean connect(SocketAddress socketAddress) throws IOException {
        return socketChannel.connect(socketAddress);
    }

    @Override
    public boolean finishConnect() throws IOException {
        return socketChannel.finishConnect();
    }

    @Override
    public SocketAddress getRemoteAddress() throws IOException {
        return socketChannel.getRemoteAddress();
    }

    @Override
    public SocketAddress getLocalAddress() throws IOException {
        return socketChannel.getLocalAddress();
    }
}
