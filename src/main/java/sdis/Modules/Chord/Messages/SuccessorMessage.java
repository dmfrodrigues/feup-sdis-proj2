package sdis.Modules.Chord.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Peer;
import sdis.Utils.DataBuilder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class SuccessorMessage extends ChordMessage<Chord.NodeInfo> {
    public SuccessorMessage(){}

    @Override
    protected DataBuilder build() {
        return new DataBuilder("SUCCESSOR".getBytes());
    }

    private static class SuccessorProcessor extends ChordMessage.Processor {

        private final SuccessorMessage message;

        public SuccessorProcessor(Chord chord, Socket socket, SuccessorMessage message){
            super(chord, socket);
            this.message = message;
        }

        @Override
        public void compute() {
            try {
                byte[] response = message.formatResponse(getChord().getSuccessor());
                getSocket().getOutputStream().write(response);
                readAllBytesAndClose(getSocket());
            } catch (IOException | InterruptedException e) {
                throw new CompletionException(e);
            }
        }
    }

    @Override
    public SuccessorProcessor getProcessor(Peer peer, Socket socket) {
        return new SuccessorProcessor(peer.getChord(), socket, this);
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
