package sdis.Modules.Chord.Messages;

import sdis.Modules.Chord.Chord;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class FingerRemoveMessage extends ChordMessage {

    private final Chord.NodeInfo oldPeer;
    private final Chord.NodeInfo newPeer;
    private final int fingerIndex;

    public FingerRemoveMessage(Chord.NodeInfo oldPeer, Chord.NodeInfo newPeer, int fingerIdx){
        this.oldPeer = oldPeer;
        this.newPeer = newPeer;
        this.fingerIndex = fingerIdx;
    }

    public FingerRemoveMessage(byte[] data){
        String dataString = new String(data);
        String[] splitString = dataString.split(" ");
        String[] splitAddressOld = splitString[2].split(":");
        String[] splitAddressNew = splitString[4].split(":");
        oldPeer = new Chord.NodeInfo(new Chord.Key(Long.parseLong(splitString[1])), new InetSocketAddress(splitAddressOld[0], Integer.parseInt(splitAddressOld[1])));
        newPeer = new Chord.NodeInfo(new Chord.Key(Long.parseLong(splitString[3])), new InetSocketAddress(splitAddressNew[0], Integer.parseInt(splitAddressNew[1])));
        fingerIndex = Integer.parseInt(splitString[5]);
    }

    public Chord.NodeInfo getOldPeerInfo(){
        return oldPeer;
    }

    public Chord.NodeInfo getNewPeerInfo(){
        return newPeer;
    }

    public int getFingerIndex(){
        return fingerIndex;
    }

    @Override
    public String toString() {
        return "FINGERREMOVE " + oldPeer + " " + newPeer + " " + getFingerIndex();
    }

    private static class FingerRemoveProcessor extends Processor {
        final FingerRemoveMessage message;

        public FingerRemoveProcessor(Chord chord, Socket socket, FingerRemoveMessage message){
            super(chord, socket);
            this.message = message;
        }

        @Override
        public Void get() {
            try {
                Chord.NodeInfo s  = getChord().getPeerInfo();
                Chord.NodeInfo r  = message.getOldPeerInfo();
                Chord.NodeInfo r_ = message.getNewPeerInfo();
                // Update fingers if necessary
                int i = message.getFingerIndex();
                boolean updatedFingers = false;
                while(
                    i >= 0 &&
                    getChord().getFinger(i).equals(r)
                ){
                    updatedFingers = true;
                    getChord().setFinger(i--, r_);
                }
                // If at least one finger was updated, redirect to predecessor
                if(updatedFingers){
                    getChord().send(getChord().getPredecessor(), message);
                }
                getSocket().close();
                return null;
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }
    }

    @Override
    public FingerRemoveProcessor getProcessor(Chord chord, Socket socket) {
        return new FingerRemoveProcessor(chord, socket, this);
    }
}
