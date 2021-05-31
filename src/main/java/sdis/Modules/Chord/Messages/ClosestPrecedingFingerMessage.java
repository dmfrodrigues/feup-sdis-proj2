package sdis.Modules.Chord.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Peer;
import sdis.Utils.DataBuilder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class ClosestPrecedingFingerMessage extends ChordMessage<Chord.NodeInfo> {

    private final Chord.Key key;

    public ClosestPrecedingFingerMessage(Chord.Key key){
        this.key = key;
    }

    public ClosestPrecedingFingerMessage(Chord chord, byte[] data){
        String dataString = new String(data);
        String[] splitString = dataString.split(" ");
        key = chord.newKey(Long.parseLong(splitString[1]));
    }

    @Override
    protected DataBuilder build() {
        return new DataBuilder(("CPFINGER " + key).getBytes());
    }

    private static class ClosestPrecedingFingerProcessor extends ChordMessage.Processor {

        private final ClosestPrecedingFingerMessage message;

        public ClosestPrecedingFingerProcessor(Chord chord, Socket socket, ClosestPrecedingFingerMessage message){
            super(chord, socket);
            this.message = message;
        }

        @Override
        public void compute() {
            Chord chord = getChord();
            Chord.NodeInfo n = chord.getNodeInfo();

            try {
                for (int i = chord.getKeySize() - 1; i >= 0; --i) {
                    Chord.NodeInfo f = chord.getFingerRaw(i);
                    if (f.key.inRange(n.key.add(1), message.key.subtract(1))) {
                        try {
                            new HelloMessage().sendTo(chord, f.createSocket());
                        } catch (IOException | InterruptedException e) {
                            continue;
                        }
                        getSocket().getOutputStream().write(message.formatResponse(f));
                        readAllBytesAndClose(getSocket());
                        return;
                    }
                }

                Chord.NodeInfo s = chord.getSuccessorInfo();
                getSocket().getOutputStream().write(message.formatResponse(s));
                readAllBytesAndClose(getSocket());
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                throw new CompletionException(e);
            }
        }
    }

    @Override
    public ClosestPrecedingFingerProcessor getProcessor(Peer peer, Socket socket) {
        return new ClosestPrecedingFingerProcessor(peer.getChord(), socket, this);
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
