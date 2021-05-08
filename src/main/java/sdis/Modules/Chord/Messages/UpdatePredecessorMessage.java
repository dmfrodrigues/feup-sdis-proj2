package sdis.Modules.Chord.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Peer;
import sdis.Utils.DataBuilder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class UpdatePredecessorMessage extends ChordMessage {

    private final Chord.NodeInfo predecessor;

    public UpdatePredecessorMessage(Chord.NodeInfo predecessor){
        this.predecessor = predecessor;
    }

    public UpdatePredecessorMessage(Chord chord, byte[] data){
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

    private Chord.NodeInfo getPredecessor() {
        return predecessor;
    }

    @Override
    protected DataBuilder build() {
        return new DataBuilder(("UPDATEPREDECESSOR " + getPredecessor()).getBytes());
    }

    private static class UpdatePredecessorProcessor extends ChordMessage.Processor {

        private final UpdatePredecessorMessage message;

        public UpdatePredecessorProcessor(Chord chord, Socket socket, UpdatePredecessorMessage message){
            super(chord, socket);
            this.message = message;
        }

        @Override
        public Void get() {
            getChord().setPredecessor(message.getPredecessor());
            try {
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
    public UpdatePredecessorProcessor getProcessor(Peer peer, Socket socket) {
        return new UpdatePredecessorProcessor(peer.getChord(), socket, this);
    }
}
