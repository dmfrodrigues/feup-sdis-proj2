package sdis.Modules.Chord.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Modules.Chord.GetPredecessorProtocol;
import sdis.Peer;
import sdis.Utils.DataBuilder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class GetPredecessorMessage extends ChordMessage<Chord.NodeInfo> {

    private final Chord.Key key;

    public GetPredecessorMessage(Chord.Key key){
        this.key = key;
    }

    public GetPredecessorMessage(Chord chord, byte[] data){
        String dataString = new String(data);
        String[] splitString = dataString.split(" ");
        key = chord.newKey(Long.parseLong(splitString[1]));
    }

    private Chord.Key getKey() {
        return key;
    }

    @Override
    protected DataBuilder build() {
        return new DataBuilder("PREDECESSOR".getBytes());
    }

    private static class PredecessorProcessor extends ChordMessage.Processor {

        private final GetPredecessorMessage message;

        public PredecessorProcessor(Chord chord, Socket socket, GetPredecessorMessage message){
            super(chord, socket);
            this.message = message;
        }

        @Override
        public void compute() {
            GetPredecessorProtocol getPredecessorProtocol = new GetPredecessorProtocol(getChord(), message.getKey());
            Chord.NodeInfo nodeInfo = getPredecessorProtocol.invoke();
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
    public PredecessorProcessor getProcessor(Peer peer, Socket socket) {
        return new PredecessorProcessor(peer.getChord(), socket, this);
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
