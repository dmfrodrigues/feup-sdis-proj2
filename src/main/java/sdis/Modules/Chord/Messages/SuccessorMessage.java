package sdis.Modules.Chord.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Peer;
import sdis.Utils.DataBuilder;
import sdis.Utils.Utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletionException;

public class SuccessorMessage extends ChordMessage<Chord.NodeInfo> {
    public SuccessorMessage(){}

    @Override
    protected DataBuilder build() {
        return new DataBuilder("SUCCESSOR".getBytes());
    }

    private static class SuccessorProcessor extends ChordMessage.Processor {

        private final SuccessorMessage message;

        public SuccessorProcessor(Chord chord, SocketChannel socket, SuccessorMessage message){
            super(chord, socket);
            this.message = message;
        }

        @Override
        public void compute() {
            try {
                Chord.NodeInfo successor = getChord().getSuccessorInfo();
                ByteBuffer response = message.formatResponse(successor);
                getSocket().write(response);
                getSocket().close();
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }
    }

    @Override
    public SuccessorProcessor getProcessor(Peer peer, SocketChannel socket) {
        return new SuccessorProcessor(peer.getChord(), socket, this);
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
