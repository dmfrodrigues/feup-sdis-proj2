package sdis.Modules.Chord.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Peer;
import sdis.Utils.DataBuilder;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class PredecessorMessage extends ChordMessage<Chord.NodeInfo> {

    @Override
    protected DataBuilder build() {
        return new DataBuilder("PREDECESSOR".getBytes());
    }

    private static class PredecessorProcessor extends ChordMessage.Processor {

        private final PredecessorMessage message;

        public PredecessorProcessor(Chord chord, Socket socket, PredecessorMessage message){
            super(chord, socket);
            this.message = message;
        }

        @Override
        public void compute() {
            try {
                byte[] response = message.formatResponse(getChord().getPredecessorInfo());
                getSocket().getOutputStream().write(response);
                readAllBytesAndClose(getSocket());
            } catch (IOException | InterruptedException e) {
                throw new CompletionException(e);
            }
        }
    }

    @Override
    public PredecessorProcessor getProcessor(Peer peer, Socket socket) {
        return new PredecessorProcessor(peer.getChord(), socket, this);
    }

    @Override
    public byte[] formatResponse(Chord.NodeInfo nodeInfo){
        return nodeInfo.toString().getBytes();
    }

    @Override
    public Chord.NodeInfo parseResponse(Chord chord, byte[] response) {
        return Chord.NodeInfo.fromString(chord, new String(response));
    }
}
