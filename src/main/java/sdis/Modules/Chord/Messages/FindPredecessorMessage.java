package sdis.Modules.Chord.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Peer;
import sdis.Utils.DataBuilder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class FindPredecessorMessage extends ChordMessage<Chord.NodeInfo> {

    private final Chord.Key key;

    public FindPredecessorMessage(Chord.Key key){
        this.key = key;
    }

    public FindPredecessorMessage(Chord chord, byte[] data){
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

    private static class FindPredecessorProcessor extends ChordMessage.Processor {

        private final FindPredecessorMessage message;

        public FindPredecessorProcessor(Chord chord, Socket socket, FindPredecessorMessage message){
            super(chord, socket);
            this.message = message;
        }

        @Override
        public void compute() {
            Chord.NodeInfo n = getChord().getNodeInfo();

            try {
                for (int i = getChord().getKeySize() - 1; i >= 0; --i) {
                    Chord.NodeInfo f = getChord().getFinger(i);
                    if (f.key.inRange(n.key, message.key)) {
                        getSocket().getOutputStream().write(message.formatResponse(f));
                        readAllBytesAndClose(getSocket());
                        return;
                    }
                }

                getSocket().getOutputStream().write(message.formatResponse(n));
                readAllBytesAndClose(getSocket());
            } catch (IOException | InterruptedException e) {
                throw new CompletionException(e);
            }
        }
    }

    @Override
    public FindPredecessorProcessor getProcessor(Peer peer, Socket socket) {
        return new FindPredecessorProcessor(peer.getChord(), socket, this);
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
