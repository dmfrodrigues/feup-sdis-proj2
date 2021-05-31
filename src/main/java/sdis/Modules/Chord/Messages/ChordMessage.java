package sdis.Modules.Chord.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Modules.Message;
import sdis.Peer;
import sdis.Utils.Utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public abstract class ChordMessage<T> extends Message {
    public static abstract class Processor extends Message.Processor {
        private final Chord chord;
        private final SocketChannel socket;

        public Processor(Chord chord, SocketChannel socket){
            this.chord = chord;
            this.socket = socket;
        }

        public Chord getChord(){
            return chord;
        }

        public SocketChannel getSocket(){
            return socket;
        }
    }

    public abstract ChordMessage.Processor getProcessor(Peer peer, SocketChannel socket);

    protected abstract ByteBuffer formatResponse(T t);

    protected abstract T parseResponse(Chord chord, ByteBuffer data);

    public T sendTo(Chord chord, InetSocketAddress address) throws IOException, InterruptedException {
        return sendTo(chord, Utils.createSocket(chord.getSSLEngine(), address));
    }

    public T sendTo(Chord chord, SocketChannel socket) throws IOException, InterruptedException {
        socket.write(this.asByteBuffer());
        return parseResponse(chord, readAllBytesAndClose(socket));
    }
}
