package sdis.Modules.Chord.Messages;

import sdis.Modules.Chord.Chord;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class UpdatePredecessorMessage extends ChordMessage {

    private final Chord.NodeInfo predecessor;

    public UpdatePredecessorMessage(Chord.NodeInfo predecessor){
        this.predecessor = predecessor;
    }

    private Chord.NodeInfo getPredecessor() {
        return predecessor;
    }

    @Override
    public String toString() {
        return "UPDATEPREDECESSOR " + getPredecessor();
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
