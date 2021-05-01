package sdis;

import sdis.Protocols.Chord.ChordMessage;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ScheduledExecutorService;

public class Chord {
    private static final ScheduledExecutorService executor = Peer.getExecutor();

    private final Peer peer;
    private final int m;
    private final long key;
    private final PeerInfo[] fingers;
    private PeerInfo predecessor;

    public Chord(Peer peer, int m, long key){
        this.peer = peer;
        this.m = m;
        this.key = key;
        fingers = new PeerInfo[this.m];
    }

    public static ScheduledExecutorService getExecutor() {
        return executor;
    }

    public long getKey() {
        return key;
    }

    public PeerInfo getFinger(int i){
        return fingers[i];
    }

    public void setFinger(int i, PeerInfo peer){
        fingers[i] = peer;
    }

    public PeerInfo getPredecessor(){
        return predecessor;
    }

    public void setPredecessor(PeerInfo peer){
        predecessor = peer;
    }

    public PeerInfo getSuccessor() {
        return getFinger(0);
    }

    public int getKeySize() {
        return m;
    }

    public PeerInfo asPeerInfo() {
        return new PeerInfo(getKey(), peer.getSocketAddress());
    }

    public Socket send(PeerInfo to, ChordMessage m) throws IOException {
        Socket socket = new Socket(to.address.getAddress(), to.address.getPort());
        OutputStream os = socket.getOutputStream();
        os.write(m.toString().getBytes());
        os.flush();
        return socket;
    }

    public PeerInfo getPeerInfo() {
        return new PeerInfo(key, peer.getSocketAddress());
    }
}
