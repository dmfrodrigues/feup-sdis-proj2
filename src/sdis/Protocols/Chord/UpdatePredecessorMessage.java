package sdis.Protocols.Chord;

import sdis.Chord;
import sdis.PeerInfo;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class UpdatePredecessorMessage extends ChordMessage {

    private final PeerInfo predecessor;

    public UpdatePredecessorMessage(PeerInfo predecessor){
        this.predecessor = predecessor;
    }

    private PeerInfo getPredecessor() {
        return predecessor;
    }

    @Override
    public String toString() {
        return "UPPREDECESSOR " + getPredecessor();
    }

    private static class UpdatePredecessorProcessor extends ChordMessage.Processor {

        private final UpdatePredecessorMessage message;

        public UpdatePredecessorProcessor(Chord chord, Socket socket, UpdatePredecessorMessage message){
            super(chord, socket);
            this.message = message;
        }

        @Override
        public Void get() {
            getChord().setPredecessor(message.getPredecessor());
            try {
                getSocket().close();
                return null;
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }
    }

    @Override
    public UpdatePredecessorProcessor getProcessor(Chord chord, Socket socket) {
        return new UpdatePredecessorProcessor(chord, socket, this);
    }
}
