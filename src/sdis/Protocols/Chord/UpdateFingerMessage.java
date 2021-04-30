package sdis.Protocols.Chord;

import sdis.PeerInfo;

import java.net.InetSocketAddress;

public class UpdateFingerMessage extends ChordMessage {

    private final PeerInfo peer;
    private final int fingerIdx;

    public UpdateFingerMessage(PeerInfo peer, int fingerIdx){
        this.peer = peer;
        this.fingerIdx = fingerIdx;
    }

    public UpdateFingerMessage(byte[] data){
        String dataString = new String(data);
        String[] splitString = dataString.split(" ");
        String[] splitAddress = splitString[2].split(":");
        peer = new PeerInfo(Long.parseLong(splitString[1]), new InetSocketAddress(splitAddress[0], Integer.parseInt(splitAddress[1])));
        fingerIdx = Integer.parseInt(splitString[3]);
    }

    @Override
    public String toString() {
        return "UPDATEFINGER " + peer + " " + fingerIdx;
    }
}
