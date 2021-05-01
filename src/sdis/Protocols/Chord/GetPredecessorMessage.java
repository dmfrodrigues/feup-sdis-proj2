package sdis.Protocols.Chord;

import sdis.Chord;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class GetPredecessorMessage extends ChordMessage {
    public GetPredecessorMessage(){}

    @Override
    public String toString() {
        return "GETPREDECESSOR";
    }

    private static class GetPredecessorProcessor extends ChordMessage.Processor {

        public GetPredecessorProcessor(Chord chord, Socket socket){
            super(chord, socket);
        }

        @Override
        public Void get() {
            try {
                byte[] response = getChord().getPredecessor().toString().getBytes();
                getSocket().getOutputStream().write(response);
                getSocket().close();
                return null;
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }
    }

    @Override
    public GetPredecessorProcessor getProcessor(Chord chord, Socket socket) {
        return new GetPredecessorProcessor(chord, socket);
    }
}
