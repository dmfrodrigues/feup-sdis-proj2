package sdis.Modules.Chord;

import sdis.Modules.Chord.Messages.GetSuccessorMessage;
import sdis.Modules.ProtocolSupplier;
import sdis.Peer;
import sdis.PeerInfo;
import sdis.Modules.Chord.Messages.ChordMessage;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class Chord {
    public static class Key {
        private final long k;

        public Key(long k){
            this.k = k % Chord.getMod();
        }

        public long toLong(){
            return k;
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
        public String toString(){
            return Long.toString(k);
        }

        @Override
        public boolean equals(Object o){
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return Objects.equals(k, key.k);
        }
    }

    private static int m = 30;

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

    public CompletableFuture<PeerInfo> getSuccessor(Chord.Key key){
        GetSuccessorProtocol getSuccessorProtocol = new GetSuccessorProtocol(this, key);
        return CompletableFuture.supplyAsync(getSuccessorProtocol, getExecutor());
    }

    public static void setKeySize(int i) {
        m = i;
    }

    public static int getKeySize() {
        return m;
    }

    public static long getMod(){
        return (1L << getKeySize());
    }

    /**
     * @brief Create a new chord system.
     *
     * @return Future that will resolve when join is complete.
     */
    public CompletableFuture<Void> join(){
        return CompletableFuture.runAsync(() -> {
            PeerInfo peerInfo = getPeerInfo();
            setPredecessor(peerInfo);
            for(int i = 0; i < getKeySize(); ++i){
                setFinger(i, peerInfo);
            }
        }, getExecutor());
    }

    /**
     * @brief Join an existing chord system.
     *
     * @param gateway
     * @param moveKeys
     * @return
     */
    public CompletableFuture<Void> join(InetSocketAddress gateway, ProtocolSupplier<Void> moveKeys) {
        JoinProtocol joinProtocol = new JoinProtocol(peer, gateway, moveKeys);
        return CompletableFuture.supplyAsync(joinProtocol, getExecutor());
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
