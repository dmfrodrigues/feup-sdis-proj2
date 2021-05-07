package sdis.Modules.Chord.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Peer;
import sdis.Utils.DataBuilder;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class GetPredecessorMessage extends ChordMessage {
    public GetPredecessorMessage(){}

    @Override
    protected DataBuilder build() {
        return new DataBuilder("GETPREDECESSOR".getBytes());
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
    public GetPredecessorProcessor getProcessor(Peer peer, Socket socket) {
        return new GetPredecessorProcessor(peer.getChord(), socket);
    }
}
