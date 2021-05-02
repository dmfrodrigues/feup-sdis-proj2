package sdis.Protocols.Chord.Messages;

import sdis.Chord;
import sdis.PeerInfo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class FingerAddMessage extends ChordMessage {

    private final PeerInfo peerInfo;
    private final int fingerIndex;

    public FingerAddMessage(PeerInfo peerInfo, int fingerIdx){
        this.peerInfo = peerInfo;
        this.fingerIndex = fingerIdx;
    }

    public FingerAddMessage(byte[] data){
        String dataString = new String(data);
        String[] splitString = dataString.split(" ");
        String[] splitAddress = splitString[2].split(":");
        peerInfo = new PeerInfo(Long.parseLong(splitString[1]), new InetSocketAddress(splitAddress[0], Integer.parseInt(splitAddress[1])));
        fingerIndex = Integer.parseInt(splitString[3]);
    }

    public PeerInfo getPeerInfo(){
        return peerInfo;
    }

    public int getFingerIndex(){
        return fingerIndex;
    }

    @Override
    public String toString() {
        return "FINGERADD " + peerInfo + " " + getFingerIndex();
    }

    private static class FingerAddProcessor extends ChordMessage.Processor {
        FingerAddMessage message;

        public FingerAddProcessor(Chord chord, Socket socket, FingerAddMessage message){
            super(chord, socket);
            this.message = message;
        }

        @Override
        public Void get() {
            try {
                PeerInfo s = getChord().getPeerInfo();
                PeerInfo r = message.getPeerInfo();
                // Update fingers if necessary
                int i = message.getFingerIndex();
                boolean updatedFingers = false;
                while(
                    i >= 0 &&
                    getChord().distance(s.key, r.key) < getChord().distance(s.key, getChord().getFinger(i).key)
                ){
                    updatedFingers = true;
                    getChord().setFinger(i--, r);
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
    public FingerAddProcessor getProcessor(Chord chord, Socket socket) {
        return new FingerAddProcessor(chord, socket, this);
    }
}
