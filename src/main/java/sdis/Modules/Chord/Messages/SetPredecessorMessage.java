package sdis.Modules.Chord.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Peer;
import sdis.Utils.DataBuilder;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletionException;

public class SetPredecessorMessage extends ChordMessage<Boolean> {

    private final Chord.NodeInfo predecessor;

    public SetPredecessorMessage(Chord.NodeInfo predecessor){
        this.predecessor = predecessor;
    }

    public SetPredecessorMessage(Chord chord, byte[] data){
        String dataString = new String(data);
        String[] splitString = dataString.split(" ");
        String[] splitAddress = splitString[2].split(":");
        predecessor = new Chord.NodeInfo(
            chord.newKey(Long.parseLong(splitString[1])),
            new InetSocketAddress(
                splitAddress[0],
                Integer.parseInt(splitAddress[1])
            )
        );
    }

    @Override
    protected DataBuilder build() {
        return new DataBuilder(("SETPREDECESSOR " + predecessor).getBytes());
    }

    private static class SetPredecessorProcessor extends ChordMessage.Processor {

        private final SetPredecessorMessage message;

        public SetPredecessorProcessor(Chord chord, SocketChannel socket, SetPredecessorMessage message){
            super(chord, socket);
            this.message = message;
        }

        @Override
        public void compute() {
            getChord().setPredecessor(message.predecessor);
            try {
                readAllBytesAndClose(getSocket());
            } catch (InterruptedException e) {
                throw new CompletionException(e);
            }
        }
    }

    @Override
    public SetPredecessorProcessor getProcessor(Peer peer, SocketChannel socket) {
        return new SetPredecessorProcessor(peer.getChord(), socket, this);
    }

    @Override
    protected ByteBuffer formatResponse(Boolean b) {
        return ByteBuffer.wrap(new byte[]{(byte) (b ? 1 : 0)});
    }

    @Override
    protected Boolean parseResponse(Chord chord, ByteBuffer data) {
        return (data.position() == 1 && data.array()[0] == 1);
    }
}
