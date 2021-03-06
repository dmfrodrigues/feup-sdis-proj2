package sdis.Modules.Chord.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Peer;
import sdis.Utils.DataBuilder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
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

        public SetPredecessorProcessor(Chord chord, Socket socket, SetPredecessorMessage message){
            super(chord, socket);
            this.message = message;
        }

        @Override
        public void compute() {
            boolean ret = getChord().setPredecessor(message.predecessor);
            try {
                getSocket().getOutputStream().write(message.formatResponse(ret));
                readAllBytesAndClose(getSocket());
            } catch (InterruptedException | IOException e) {
                throw new CompletionException(e);
            }
        }
    }

    @Override
    public SetPredecessorProcessor getProcessor(Peer peer, Socket socket) {
        return new SetPredecessorProcessor(peer.getChord(), socket, this);
    }

    @Override
    protected byte[] formatResponse(Boolean b) {
        return new byte[]{(byte) (b ? 1 : 0)};
    }

    @Override
    protected Boolean parseResponse(Chord chord, byte[] data) {
        return (data.length == 1 && data[0] == 1);
    }
}
