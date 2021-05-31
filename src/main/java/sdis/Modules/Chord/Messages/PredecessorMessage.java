package sdis.Modules.Chord.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Peer;
import sdis.Utils.DataBuilder;
import sdis.Utils.Utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletionException;

public class PredecessorMessage extends ChordMessage<Chord.NodeInfo> {

    @Override
    protected DataBuilder build() {
        return new DataBuilder("PREDECESSOR".getBytes());
    }

    private static class PredecessorProcessor extends ChordMessage.Processor {

        private final PredecessorMessage message;

        public PredecessorProcessor(Chord chord, SocketChannel socket, PredecessorMessage message){
            super(chord, socket);
            this.message = message;
        }

        @Override
        public void compute() {
            try {
                ByteBuffer response = message.formatResponse(getChord().getPredecessorInfo());
                getSocket().write(response);
                readAllBytesAndClose(getSocket());
            } catch (IOException | InterruptedException e) {
                throw new CompletionException(e);
            }
        }
    }

    @Override
    public PredecessorProcessor getProcessor(Peer peer, SocketChannel socket) {
        return new PredecessorProcessor(peer.getChord(), socket, this);
    }

    @Override
    public ByteBuffer formatResponse(Chord.NodeInfo nodeInfo){
        return ByteBuffer.wrap(nodeInfo.toString().getBytes());
    }

    @Override
    public Chord.NodeInfo parseResponse(Chord chord, ByteBuffer response) {
        return Chord.NodeInfo.fromString(chord, Utils.fromByteBufferToString(response));
    }
}
