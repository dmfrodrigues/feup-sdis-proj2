package sdis.Sockets;

import sdis.Modules.Main.Main;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SecureSocketChannel extends SocketChannel {
    private final static byte FLAG = 0x7E;
    private final static byte ESC  = 0x7D;
    private final static byte XOR  = 0x20;

    private static final ByteStuffer byteStuffer = new ByteStuffer(FLAG, ESC, XOR);

    private final SSLEngine engine;
    private final SocketChannel socketChannel;

    private ByteBuffer appData     = ByteBuffer.allocate(Main.CHUNK_SIZE + Main.MAX_HEADER_SIZE);   /** @brief Plaintext data **/
    private ByteBuffer netData;                                                                     /** @brief Encrypted data **/
    private ByteBuffer peerAppData = ByteBuffer.allocate(Main.CHUNK_SIZE + Main.MAX_HEADER_SIZE);
    private ByteBuffer peerNetData;

    private final ExecutorService executor = Executors.newFixedThreadPool(100);

    public SecureSocketChannel(SocketChannel socketChannel, SSLEngine engine) throws IOException {
        super(socketChannel.provider());
        this.engine = engine;
        this.socketChannel = socketChannel;
        initializeNetBuffers();
        this.engine.beginHandshake();
        handshake();
    }

    private void initializeNetBuffers() {
        SSLSession session = engine.getSession();
        netData = ByteBuffer.allocate(session.getPacketBufferSize());
        peerNetData = ByteBuffer.allocate(session.getPacketBufferSize());
    }

    private boolean handshake() throws IOException {
//        System.out.println(s + ": Starting handshake");

        netData.clear();
        peerNetData.clear();

        SSLEngineResult.HandshakeStatus hs = engine.getHandshakeStatus();
        SSLEngineResult res;

        // Process handshaking message
        while (hs != SSLEngineResult.HandshakeStatus.FINISHED &&
                hs != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {

//            System.out.println(s + ": hs = " + hs);

            switch (hs) {
                case NEED_UNWRAP:
                    // Receive handshaking data from peer
//                    System.out.println(s + ": UNWRAPPING");
                    if (socketChannel.read(peerNetData) < 0) {
                        // Channel reached end-of-stream
//                        System.out.println(s + ": L87");
                        if(engine.isOutboundDone() && engine.isInboundDone())
                            return false;
                        try {
                            engine.closeInbound();
                        } catch (SSLException e) {
                            e.printStackTrace();
                        }
                        engine.closeOutbound();
                        hs = engine.getHandshakeStatus();
                        break;
                    }
//                    System.out.println(s + ": UNWRAPPED");

                    // Process incoming handshaking data
                    peerNetData.flip();
                    try {
                        res = engine.unwrap(peerNetData, peerAppData);
                        peerNetData.compact();
                        hs = res.getHandshakeStatus();
                    } catch (SSLException e) {
                        e.printStackTrace();
                        engine.closeOutbound();
                        hs = engine.getHandshakeStatus();
                        break;
                    }
                    switch (res.getStatus()) {
                        case OK :
                            break;
                        case BUFFER_OVERFLOW:
                            peerAppData = enlargeBuffer(peerAppData, engine.getSession().getApplicationBufferSize());
                            break;
                        case BUFFER_UNDERFLOW:
                            peerNetData = handleBufferUnderflow(engine, peerNetData);
                            break;
                        case CLOSED:
//                            System.out.println(s + ": L134");
                            if (engine.isOutboundDone())
                                return false;
                            engine.closeOutbound();
                            hs = engine.getHandshakeStatus();
                            break;
                        default:
                            throw new IllegalStateException("Invalid SSL status: " + res.getStatus());
                    }
                    break;
                case NEED_WRAP:
                    netData.clear();
                    try {
                        res = engine.wrap(appData, netData);
                        hs = res.getHandshakeStatus();
                    } catch (SSLException e) {
                        e.printStackTrace();
                        engine.closeOutbound();
                        hs = engine.getHandshakeStatus();
                        break;
                    }
                    switch (res.getStatus()) {
                        case OK :
                            netData.flip();
                            while (netData.hasRemaining()) {
                                socketChannel.write(netData);
                            }
                            break;
                        case BUFFER_OVERFLOW:
                            netData = enlargeBuffer(netData, engine.getSession().getPacketBufferSize());
                            break;
                        case BUFFER_UNDERFLOW:
                            throw new SSLException("Buffer underflow occured after a wrap. I don't think we should ever get here.");
                        case CLOSED:
                            if(engine.isOutboundDone()){ hs = engine.getHandshakeStatus(); break; }
                            netData.flip();
                            while (netData.hasRemaining())
                                socketChannel.write(netData);
                            break;
                        default:
                            throw new IllegalStateException("Invalid SSL status: " + res.getStatus());
                    }
                    break;
                case NEED_TASK :
                    // Handle blocking tasks
                    Runnable task;
                    while ((task = engine.getDelegatedTask()) != null) {
                        executor.execute(task);
                    }
                    hs = engine.getHandshakeStatus();
                    break;
                case FINISHED:
                    break;
                case NOT_HANDSHAKING:
                    break;
                default:
                    throw new IllegalStateException("Invalid SSL status: " + hs);
            }
        }

//        System.out.println(s + ": Done handshaking, handshake status is " + hs);

        peerAppData.clear();
        appData.clear();

        return true;
    }

    protected ByteBuffer enlargeBuffer(ByteBuffer buffer, int sessionProposedCapacity) {
        if (sessionProposedCapacity > buffer.capacity()) {
            buffer = ByteBuffer.allocate(sessionProposedCapacity);
        } else {
            buffer = ByteBuffer.allocate(buffer.capacity() * 2);
        }
        return buffer;
    }

    protected ByteBuffer handleBufferUnderflow(SSLEngine engine, ByteBuffer buffer) {
        if (engine.getSession().getPacketBufferSize() < buffer.limit()) {
            return buffer;
        } else {
            ByteBuffer replaceBuffer = enlargeBuffer(buffer, engine.getSession().getPacketBufferSize());
            buffer.flip();
            replaceBuffer.put(buffer);
            return replaceBuffer;
        }
    }

    /*
    protected void closeConnection() throws IOException  {
        engine.closeOutbound();
        handshake();
        socketChannel.close();
    }
     */

    /*
    protected void handleEndOfStream() throws IOException  {
        try {
            engine.closeInbound();
        } catch (Exception e) {
            e.printStackTrace();
        }
        closeConnection();
    }
     */

    private void doWrite(ByteBuffer src) throws IOException {
        // System.out.println("Called doWrite [" + new String(src.array(), 0, src.limit()) + "]");

        appData.clear();

        // Copy app data from src to appData
        if(src.remaining() > appData.remaining()){
            appData = ByteBuffer.allocate(src.remaining() * 2);
        }
        appData.put(src);
        appData.put(FLAG);
        appData.flip();

        // Process app data
        while(appData.hasRemaining()){
            netData.clear();
            SSLEngineResult res = engine.wrap(appData, netData);

//            System.out.println("doWrite, res=" + res.getStatus());

            switch (res.getStatus()){
                case OK:
                    netData.flip();
                    while(netData.hasRemaining()){
                        socketChannel.write(netData);
                    }
                    // System.out.println("Wrote everything");
                    break;
                case BUFFER_OVERFLOW:
                    netData = ByteBuffer.allocate(netData.capacity() * 2);
                    break;
                case CLOSED: // Close connection
                    engine.closeOutbound();
                    handshake();
                    socketChannel.close();
                    throw new ClosedChannelException();
                default:
                    throw new IllegalStateException("Unexpected SSL state " + res.getStatus());
            }
        }

//        System.out.println("Leaving doWrite");
    }

    public void doRead(ByteBuffer messageBuffer) throws IOException {
//        System.out.println("Asked to read");

        while(socketChannel.read(peerNetData) >= 0) {
            peerNetData.flip();
            LABEL_NETDATA_HASREMAINING:
            {
                while (peerNetData.hasRemaining()) {
                    SSLEngineResult res = engine.unwrap(peerNetData, peerAppData);

//                    System.out.println("doRead, res = " + res.getStatus());

                    switch (res.getStatus()) {
                        case OK:
                            peerAppData.flip();
                            if (peerAppData.remaining() > messageBuffer.remaining()) throw new BufferOverflowException();

                            while (peerAppData.hasRemaining()) {
                                byte b = peerAppData.get();
                                if (b == FLAG) return;
                                messageBuffer.put(b);
                            }

                            peerAppData.compact();
                            break;
                        case BUFFER_UNDERFLOW:
                            break LABEL_NETDATA_HASREMAINING;
                        case BUFFER_OVERFLOW:
                            peerAppData = enlargeBuffer(peerAppData, engine.getSession().getPacketBufferSize());
                            break;
                        case CLOSED:
                            engine.closeOutbound();
                            handshake();
                            socketChannel.close();
                            break;
                        default:
                            throw new IllegalStateException("Unexpected SSL state " + res.getStatus());
                    }
                }
            }
            peerNetData.compact();
        }

        throw new IllegalStateException();

//        System.out.println("STRANGE...");
    }

    /** Read/write methods **/

    @Override
    public int read(ByteBuffer byteBuffer) throws IOException {
        ByteBuffer stuffed = ByteBuffer.allocate(byteBuffer.capacity() * 2);
        doRead(stuffed);
        int initialPosition = byteBuffer.position();
        byteStuffer.unstuff(stuffed, byteBuffer);
        return byteBuffer.position() - initialPosition;
    }

    @Override
    public long read(ByteBuffer[] byteBuffers, int i, int i1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int write(ByteBuffer byteBuffer) throws IOException {
        int initialPosition = byteBuffer.position();
        ByteBuffer stuffed = byteStuffer.stuff(byteBuffer);
        doWrite(stuffed);
        return byteBuffer.position() - initialPosition;
    }

    @Override
    public long write(ByteBuffer[] byteBuffers, int i, int i1) throws IOException {
        throw new UnsupportedOperationException();
    }

    /** Protected calls; redirect to socketChannel but with some more stuff **/

    @Override
    protected void implCloseSelectableChannel() throws IOException {
//        ByteBuffer empty = ByteBuffer.allocate(0);

        // Call and process closeOutbound
        engine.closeOutbound();
//        LABEL_CLOSE_LOOP_OUTBOUND: while (!engine.isOutboundDone()) {
//            SSLEngineResult res = engine.wrap(empty, netData);  // Get close message
//            System.out.println("res = " + res.getStatus());
//            switch (res.getStatus()) {
//                case OK: // Send close message to peer
//                    netData.flip();
//                    while(netData.hasRemaining()) {
//                        socketChannel.write(netData);
//                    }
//                    netData.compact();
//                    break;
//                case CLOSED: break LABEL_CLOSE_LOOP_OUTBOUND;
//                case BUFFER_OVERFLOW: netData = ByteBuffer.allocate(netData.capacity() * 2); break;
//                default: throw new IllegalStateException("Unexpected SSL state " + res.getStatus());
//            }
//        }
        handshake();

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
