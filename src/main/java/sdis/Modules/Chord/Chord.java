package sdis.Modules.Chord;

import sdis.Peer;
import sdis.PeerInfo;
import sdis.Modules.Chord.Messages.ChordMessage;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.Executor;

public class Chord {
    public static class Key implements Comparable<Key>{
        private final long k;

        public Key(long k){
            this.k = k % Chord.getMod();
        }

        public Key add(long l) {
            return new Key(k + l);
        }

        public long subtract(Key key){
            return k - key.k;
        }

        public Key subtract(long l){
            return new Key(k - l);
        }

        @Override
        public int compareTo(Key key) {
            return Long.compare(k, key.k);
        }
    }

    private final static int m = 62;

    private final Peer peer;
    private final Executor executor;
    private final Chord.Key key;
    private final PeerInfo[] fingers;
    private PeerInfo predecessor;

    public Chord(Peer peer, Executor executor, Chord.Key key){
        this.peer = peer;
        this.executor = executor;
        this.key = key;
        fingers = new PeerInfo[this.m];
    }

    public Executor getExecutor() {
        return executor;
    }

    public Chord.Key getKey() {
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

    public static int getKeySize() {
        return m;
    }

    public static long getMod(){
        return (1L << getKeySize());
    }


    public Socket send(InetSocketAddress to, ChordMessage m) throws IOException {
        Socket socket = new Socket(to.getAddress(), to.getPort());
        OutputStream os = socket.getOutputStream();
        os.write(m.toString().getBytes());
        os.flush();
        return socket;
    }

    public Socket send(PeerInfo to, ChordMessage m) throws IOException {
        return send(to.address, m);
    }

    public PeerInfo getPeerInfo() {
        return new PeerInfo(key, peer.getSocketAddress());
    }

    public Peer getPeer(){ return peer; }

    public long distance(Chord.Key a, Chord.Key b) {
        long MOD = 1L << getKeySize();
        return (b.subtract(a) + MOD) % MOD;
    }
}
