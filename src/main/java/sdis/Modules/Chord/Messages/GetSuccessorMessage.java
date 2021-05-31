package sdis.Modules.Chord.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Modules.Chord.GetSuccessorProtocol;
import sdis.Peer;
import sdis.Utils.DataBuilder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletionException;

public class GetSuccessorMessage extends ChordMessage<Chord.NodeInfo> {

    private final Chord.Key key;

    public GetSuccessorMessage(Chord.Key key){
        this.key = key;
    }

    public GetSuccessorMessage(Chord chord, byte[] data){
        String dataString = new String(data);
        String[] splitString = dataString.split(" ");
        key = chord.newKey(Long.parseLong(splitString[1]));
    }

    @Override
    protected DataBuilder build() {
        return new DataBuilder(("GETSUCCESSOR " + key).getBytes());
    }

    private static class GetSuccessorProcessor extends ChordMessage.Processor {

        private final GetSuccessorMessage message;

        public GetSuccessorProcessor(Chord chord, Socket socket, GetSuccessorMessage message){
            super(chord, socket);
            this.message = message;
        }

        @Override
        public void compute() {
            GetSuccessorProtocol getSuccessorProtocol = new GetSuccessorProtocol(getChord(), message.key);
            Chord.NodeInfo nodeInfo = getSuccessorProtocol.invoke();
            try {
                byte[] response = message.formatResponse(nodeInfo);
                getSocket().getOutputStream().write(response);
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
    public byte[] formatResponse(Chord.NodeInfo nodeInfo){
        return nodeInfo.toString().getBytes();
    }

    @Override
    public Chord.NodeInfo parseResponse(Chord chord, byte[] response) {
        String dataString = new String(response);
        String[] splitString = dataString.split(" ");
        Chord.Key key = chord.newKey(Long.parseLong(splitString[0]));
        String[] splitAddress = splitString[1].split(":");
        InetSocketAddress address = new InetSocketAddress(splitAddress[0], Integer.parseInt(splitAddress[1]));
        return new Chord.NodeInfo(key, address);
    }
}
