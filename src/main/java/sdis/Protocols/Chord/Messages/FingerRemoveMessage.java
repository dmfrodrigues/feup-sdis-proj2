package sdis.Protocols.Chord.Messages;

import sdis.Chord;
import sdis.PeerInfo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class FingerRemoveMessage extends ChordMessage {

    private final PeerInfo oldPeer;
    private final PeerInfo newPeer;
    private final int fingerIndex;

    public FingerRemoveMessage(PeerInfo oldPeer, PeerInfo newPeer, int fingerIdx){
        this.oldPeer = oldPeer;
        this.newPeer = newPeer;
        this.fingerIndex = fingerIdx;
    }

    public FingerRemoveMessage(byte[] data){
        String dataString = new String(data);
        String[] splitString = dataString.split(" ");
        String[] splitAddressOld = splitString[2].split(":");
        String[] splitAddressNew = splitString[4].split(":");
        oldPeer = new PeerInfo(Long.parseLong(splitString[1]), new InetSocketAddress(splitAddressOld[0], Integer.parseInt(splitAddressOld[1])));
        newPeer = new PeerInfo(Long.parseLong(splitString[3]), new InetSocketAddress(splitAddressNew[0], Integer.parseInt(splitAddressNew[1])));
        fingerIndex = Integer.parseInt(splitString[5]);
    }

    public PeerInfo getOldPeerInfo(){
        return oldPeer;
    }

    public PeerInfo getNewPeerInfo(){
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
        FingerRemoveMessage message;

        public FingerRemoveProcessor(Chord chord, Socket socket, FingerRemoveMessage message){
            super(chord, socket);
            this.message = message;
        }

        @Override
        public Void get() {
            try {
                PeerInfo s  = getChord().getPeerInfo();
                PeerInfo r  = message.getOldPeerInfo();
                PeerInfo r_ = message.getNewPeerInfo();
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
