package sdis;

import sdis.Modules.Chord.Chord;

import java.net.InetSocketAddress;

public class PeerInfo {
    public Chord.Key key;
    public InetSocketAddress address;

    public PeerInfo(Chord.Key key, InetSocketAddress address){
        this.key = key;
        this.address = address;
    }

    public PeerInfo(byte[] data) {
        String dataString = new String(data);
        String[] splitString = dataString.split(" ");
        key = new Chord.Key(Long.parseLong(splitString[0]));
        String[] splitAddress = splitString[1].split(":");
        address = new InetSocketAddress(splitAddress[0], Integer.parseInt(splitAddress[1]));
    }

    public String toString() {
        return key + " " + address.getAddress().getHostAddress() + ":" + address.getPort();
    }

    public boolean equals(PeerInfo obj){
        return (key == obj.key && address.equals(obj.address));
    }
}
