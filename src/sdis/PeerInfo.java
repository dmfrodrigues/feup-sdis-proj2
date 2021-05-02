package sdis;

import java.net.InetSocketAddress;

public class PeerInfo {
    public long key;
    public InetSocketAddress address;

    public PeerInfo(long key, InetSocketAddress address){
        this.key = key;
        this.address = address;
    }

    public PeerInfo(byte[] data) {
        String dataString = new String(data);
        String[] splitString = dataString.split(" ");
        key = Long.parseLong(splitString[0]);
        String[] splitAddress = splitString[1].split(":");
        address = new InetSocketAddress(splitAddress[0], Integer.parseInt(splitAddress[1]));
    }

    public String toString() {
        return key + " " + address.getAddress().getHostAddress() + ":" + address.getPort();
    }
}
