package sdis.Modules.Chord.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Peer;
import sdis.Utils.DataBuilder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class GetPredecessorMessage extends ChordMessage {
    public GetPredecessorMessage(){}

    @Override
    protected DataBuilder build() {
        return new DataBuilder("GETPREDECESSOR".getBytes());
    }

    private static class GetPredecessorProcessor extends ChordMessage.Processor {

        private final GetPredecessorMessage message;

        public GetPredecessorProcessor(Chord chord, Socket socket, GetPredecessorMessage message){
            super(chord, socket);
            this.message = message;
        }

        @Override
        public Void get() {
            try {
                byte[] response = message.formatResponse(getChord().getPredecessor());
                getSocket().getOutputStream().write(response);
                getSocket().shutdownOutput();
                getSocket().getInputStream().readAllBytes();
                getSocket().close();
                return null;
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }
    }

    @Override
    public GetPredecessorProcessor getProcessor(Peer peer, Socket socket) {
        return new GetPredecessorProcessor(peer.getChord(), socket, this);
    }

    public byte[] formatResponse(Chord.NodeInfo nodeInfo){
        return nodeInfo.toString().getBytes();
    }

    public Chord.NodeInfo parseResponse(Chord chord, byte[] response) {
        String dataString = new String(response);
        String[] splitString = dataString.split(" ");
        Chord.Key key = chord.newKey(Long.parseLong(splitString[0]));
        String[] splitAddress = splitString[1].split(":");
        InetSocketAddress address = new InetSocketAddress(splitAddress[0], Integer.parseInt(splitAddress[1]));
        return new Chord.NodeInfo(key, address);
    }
}
