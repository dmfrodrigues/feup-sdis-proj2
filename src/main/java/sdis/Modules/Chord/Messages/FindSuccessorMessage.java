package sdis.Modules.Chord.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Modules.Chord.FindSuccessorProtocol;
import sdis.Peer;
import sdis.Utils.DataBuilder;
import sdis.Utils.Utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletionException;

public class FindSuccessorMessage extends ChordMessage<Chord.NodeInfo> {

    private final Chord.Key key;

    public FindSuccessorMessage(Chord.Key key){
        this.key = key;
    }

    public FindSuccessorMessage(Chord chord, byte[] data){
        String dataString = new String(data);
        String[] splitString = dataString.split(" ");
        key = chord.newKey(Long.parseLong(splitString[1]));
    }

    @Override
    protected DataBuilder build() {
        return new DataBuilder(("FINDSUCCESSOR " + key).getBytes());
    }

    private static class GetSuccessorProcessor extends ChordMessage.Processor {

        private final FindSuccessorMessage message;

        public GetSuccessorProcessor(Chord chord, SocketChannel socket, FindSuccessorMessage message){
            super(chord, socket);
            this.message = message;
        }

        @Override
        public void compute() {
            FindSuccessorProtocol findSuccessorProtocol = new FindSuccessorProtocol(getChord(), message.key);
            Chord.NodeInfo nodeInfo = findSuccessorProtocol.invoke();
            try {
                ByteBuffer response = message.formatResponse(nodeInfo);
                getSocket().write(response);
                readAllBytesAndClose(getSocket());
            } catch (IOException | InterruptedException e) {
                throw new CompletionException(e);
            }
        }
    }

    @Override
    public GetSuccessorProcessor getProcessor(Peer peer, SocketChannel socket) {
        return new GetSuccessorProcessor(peer.getChord(), socket, this);
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
