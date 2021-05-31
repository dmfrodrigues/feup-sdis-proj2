package sdis.Sockets;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public abstract class SSLsocket {

    /* Plaintext data */
    protected ByteBuffer appData;
    /* Encrypted data */
    protected ByteBuffer netData;
    protected ByteBuffer peerAppData;
    protected ByteBuffer peerNetData;


    public ByteBuffer getAppData() {
        return appData;
    }

    public ByteBuffer getNetData() {
        return netData;
    }

    public ByteBuffer getPeerAppData() {
        return peerAppData;
    }

    public ByteBuffer getPeerNetData() {
        return peerNetData;
    }

    protected boolean handshake(SocketChannel socketChannel, SSLEngine engine) throws IOException {
        // Create byte buffers to use for holding application data
        int appBufferSize = engine.getSession().getApplicationBufferSize();
        ByteBuffer myAppData = ByteBuffer.allocate(appBufferSize);
        ByteBuffer peerAppData = ByteBuffer.allocate(appBufferSize);
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
                    res = engine.wrap(myAppData, netData);
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
                    while ((task=engine.getDelegatedTask()) != null) {
                        new Thread(task).start();
                    }
                    break;
                default: // FINISHED or NOT_HANDSHAKING
                    break;
            }
        }
        return true;
    }

}
