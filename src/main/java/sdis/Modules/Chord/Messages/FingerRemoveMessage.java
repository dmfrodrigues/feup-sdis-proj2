package sdis.Modules.Chord.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Peer;
import sdis.Utils.DataBuilder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class FingerRemoveMessage extends ChordMessage<Void> {

    private final Chord.NodeInfo oldPeer;
    private final Chord.NodeInfo newPeer;
    private final int fingerIndex;

    public FingerRemoveMessage(Chord.NodeInfo oldPeer, Chord.NodeInfo newPeer, int fingerIdx){
        this.oldPeer = oldPeer;
        this.newPeer = newPeer;
        this.fingerIndex = fingerIdx;
    }

    public FingerRemoveMessage(Chord chord, byte[] data){
        String dataString = new String(data);
        String[] splitString = dataString.split(" ");
        String[] splitAddressOld = splitString[2].split(":");
        String[] splitAddressNew = splitString[4].split(":");
        oldPeer = new Chord.NodeInfo(chord.newKey(Long.parseLong(splitString[1])), new InetSocketAddress(splitAddressOld[0], Integer.parseInt(splitAddressOld[1])));
        newPeer = new Chord.NodeInfo(chord.newKey(Long.parseLong(splitString[3])), new InetSocketAddress(splitAddressNew[0], Integer.parseInt(splitAddressNew[1])));
        fingerIndex = Integer.parseInt(splitString[5]);
    }

    @Override
    protected DataBuilder build() {
        return new DataBuilder(("FINGERREMOVE " + oldPeer + " " + newPeer + " " + fingerIndex).getBytes());
    }

    private static class FingerRemoveProcessor extends Processor {
        final FingerRemoveMessage message;

        public FingerRemoveProcessor(Chord chord, Socket socket, FingerRemoveMessage message){
            super(chord, socket);
            this.message = message;
        }

        @Override
        public void compute() {
            try {
                Chord chord = getChord();
                Chord.NodeInfo n = chord.getNodeInfo();
                Chord.NodeInfo sOld = message.oldPeer;
                Chord.NodeInfo sNew = message.newPeer;
                Chord.NodeInfo p = chord.getPredecessor();

                // If the new node to update the fingers table is itself, ignore
                if(n.equals(sOld)){
                    getSocket().shutdownOutput();
                    getSocket().getInputStream().readAllBytes();
                    getSocket().close();
                    return;
                }

                // Update fingers if necessary
                int i = message.fingerIndex;
                boolean updatedFingers = false;
                while(
                    i >= 0 &&
                    getChord().getFinger(i).equals(sOld)
                ){
                    updatedFingers = true;
                    getChord().setFinger(i--, sNew);
                }

                // If at least one finger was updated, and the predecessor was
                // not the one that sent the message, redirect to predecessor.
                // (this is already prevented by the `r.equals(fOld)` check on
                // arrival, but we can also check that on departure)
                if(updatedFingers && !p.equals(sOld)){
                    message.sendTo(chord, p.address);
                }

                readAllBytesAndClose(getSocket());
            } catch (IOException | InterruptedException e) {
                throw new CompletionException(e);
            }
        }
    }

    @Override
    public FingerRemoveProcessor getProcessor(Peer peer, Socket socket) {
        return new FingerRemoveProcessor(peer.getChord(), socket, this);
    }

    @Override
    protected byte[] formatResponse(Void unused) {
        return null;
    }

    @Override
    protected Void parseResponse(Chord chord, byte[] data) {
        return null;
    }
}
