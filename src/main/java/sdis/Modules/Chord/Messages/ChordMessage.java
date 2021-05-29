package sdis.Modules.Chord.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Modules.Message;
import sdis.Peer;

import java.net.Socket;

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

    public abstract ChordMessage.Processor getProcessor(Peer peer, Socket socket);

    protected abstract byte[] formatResponse(T t);

    protected abstract T parseResponse(Chord chord, byte[] data);
}
