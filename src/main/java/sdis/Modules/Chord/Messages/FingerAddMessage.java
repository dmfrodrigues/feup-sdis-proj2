package sdis.Modules.Chord.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Peer;
import sdis.Utils.DataBuilder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class FingerAddMessage extends ChordMessage {

    private final Chord.NodeInfo nodeInfo;
    private final int fingerIndex;

    public FingerAddMessage(Chord.NodeInfo nodeInfo, int fingerIdx){
        this.nodeInfo = nodeInfo;
        this.fingerIndex = fingerIdx;
    }

    public FingerAddMessage(Chord chord, byte[] data){
        String dataString = new String(data);
        String[] splitString = dataString.split(" ");
        String[] splitAddress = splitString[2].split(":");
        nodeInfo = new Chord.NodeInfo(
            chord.newKey(Long.parseLong(splitString[1])),
            new InetSocketAddress(
                splitAddress[0],
                Integer.parseInt(splitAddress[1])
            )
        );
        fingerIndex = Integer.parseInt(splitString[3]);
    }

    public Chord.NodeInfo getPeerInfo(){
        return nodeInfo;
    }

    public int getFingerIndex(){
        return fingerIndex;
    }

    @Override
    protected DataBuilder build() {
        return new DataBuilder(("FINGERADD " + nodeInfo + " " + getFingerIndex()).getBytes());
    }

    private static class FingerAddProcessor extends ChordMessage.Processor {
        final FingerAddMessage message;

        public FingerAddProcessor(Chord chord, Socket socket, FingerAddMessage message){
            super(chord, socket);
            this.message = message;
        }

        @Override
        public void compute() {
            try {
                Chord chord = getChord();
                Chord.NodeInfo r = chord.getNodeInfo();
                Chord.NodeInfo f = message.getPeerInfo();
                Chord.NodeInfo p = chord.getPredecessor();

                // If the new node to update the fingers table is itself, ignore
                if(r.equals(f)){
                    readAllBytesAndClose(getSocket());
                    return;
                }

                // Update fingers if necessary
                int i = message.getFingerIndex();
                boolean updatedFingers = false;
                while(
                    i >= 0 &&
                    Chord.distance(r.key.add(1L << i), f.key) < Chord.distance(r.key.add(1L << i), chord.getFinger(i).key)
                ){
                    updatedFingers = true;
                    chord.setFinger(i--, f);
                }

                // If at least one finger was updated, and the predecessor was
                // not the one that sent the message, redirect to predecessor.
                // (this is already prevented by the `r.equals(f)` check on
                // arrival, but we can also check that on departure)
                if(updatedFingers && !p.equals(f)){
                    Socket predecessorSocket = chord.send(p, message);
                    readAllBytesAndClose(predecessorSocket);
                }

                readAllBytesAndClose(getSocket());
            } catch (IOException | InterruptedException e) {
                throw new CompletionException(e);
            }
        }
    }

    @Override
    public FingerAddProcessor getProcessor(Peer peer, Socket socket) {
        return new FingerAddProcessor(peer.getChord(), socket, this);
    }
}
