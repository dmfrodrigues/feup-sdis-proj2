package sdis.Modules.Chord.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Modules.Message;
import sdis.Peer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;

public abstract class ChordMessage<T> extends Message {
    public static abstract class Processor extends Message.Processor {
        private final Chord chord;
        private final Socket socket;

        public Processor(Chord chord, Socket socket){
            this.chord = chord;
            this.socket = socket;
        }

        public Chord getChord(){
            return chord;
        }

        public Socket getSocket(){
            return socket;
        }
    }

    public abstract ChordMessage.Processor getProcessor(Peer peer, SocketChannel socket);

    protected abstract byte[] formatResponse(T t);

    protected abstract T parseResponse(Chord chord, byte[] data);

    public T sendTo(Chord chord, InetSocketAddress address) throws IOException, InterruptedException {
        return parseResponse(chord, readAllBytesAndClose(chord.send(address, this)));
    }
}
