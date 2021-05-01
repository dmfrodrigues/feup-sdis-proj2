package sdis.Protocols.Chord.Messages;

import sdis.Chord;

import java.net.Socket;
import java.util.function.Supplier;

public abstract class ChordMessage {
    public abstract String toString();

    public static abstract class Processor implements Supplier<Void> {
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

    public abstract Processor getProcessor(Chord chord, Socket socket);
}
