package sdis.Modules.Chord;

import sdis.Modules.Chord.Messages.ChordMessage;
import sdis.Modules.ProtocolTask;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.CompletionException;

public class Chord {
    public static class Key {
        private final Chord chord;
        private final long k;

        private Key(Chord chord, long k){
            this.chord = chord;
            long MOD = chord.getMod();
            this.k = ((k % MOD) + MOD) % MOD;
        }

        public long toLong(){
            return k;
        }

        public Key add(long l) {
            return new Key(chord, k + l);
        }

        public long subtract(Key key){
            return k - key.k;
        }

        public Key subtract(long l){
            return new Key(chord, k - l);
        }

        /**
         * @brief Checks if the key is in range [a, b].
         *
         * @param a     Left limit of range
         * @param b     Right limit of range
         * @return      True if in range, false otherwise
         */
        public boolean inRange(Key a, Key b) {
            long d1 = Chord.distance(a, this);
            long d2 = Chord.distance(a, b);
            return (d1 <= d2);
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

    public static class NodeInfo {
        public Chord.Key key;
        public InetSocketAddress address;

        public NodeInfo(){}

        public NodeInfo(Chord.Key key, InetSocketAddress address){
            this.key = key;
            this.address = address;
        }

        public NodeInfo(NodeInfo nodeInfo){
            this(nodeInfo.key, nodeInfo.address);
        }

        public void copy(NodeInfo nodeInfo){
            key     = nodeInfo.key;
            address = nodeInfo.address;
        }

        public Socket createSocket() throws IOException {
            return new Socket(address.getAddress(), address.getPort());
        }

        @Override
        public String toString() {
            return key + " " + address.getAddress().getHostAddress() + ":" + address.getPort();
        }

        @Override
        public boolean equals(Object o){
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NodeInfo info = (NodeInfo) o;
            return (key.equals(info.key) && address.equals(info.address));
        }
    }

    private final int keySize;

    private final InetSocketAddress socketAddress;
    private final Chord.Key key;
    private final NodeInfo[] fingers;
    private final NodeInfo predecessor;

    public Chord(InetSocketAddress socketAddress, int keySize, long key){
        this.socketAddress = socketAddress;
        this.keySize = keySize;
        this.key = newKey(key);
        fingers = new NodeInfo[this.keySize];
        predecessor = new NodeInfo();
    }

    public Chord.Key newKey(long k){
        return new Chord.Key(this, k);
    }

    public Chord.Key getKey() {
        return key;
    }

    public NodeInfo getFinger(int i){
        synchronized(fingers) {
            return fingers[i];
        }
    }

    public boolean setFinger(int i, NodeInfo peer){
        synchronized(fingers) {
            fingers[i] = peer;
        }
        return true;
    }

    public NodeInfo getPredecessor(){
        synchronized(predecessor) {
            return new NodeInfo(predecessor);
        }
    }

    public NodeInfo findPredecessor(Chord.Key key){
        return new FindPredecessorProtocol(this, key).invoke();
    }

    public boolean setPredecessor(NodeInfo peer){
        synchronized(predecessor) {
            predecessor.copy(peer);
        }
        return true;
    }

    public NodeInfo getSuccessor() {
        return getFinger(0);
    }

    public NodeInfo findSuccessor(Chord.Key key){
        return new FindSuccessorProtocol(this, key).invoke();
    }

    public int getKeySize() {
        return keySize;
    }

    public long getMod(){
        return (1L << getKeySize());
    }

    public NodeInfo getNodeInfo() {
        return new NodeInfo(key, socketAddress);
    }

    /**
     * @brief Create a new chord system.
     */
    public boolean join(){
        NodeInfo nodeInfo = getNodeInfo();
        if(!setPredecessor(nodeInfo)) return false;
        for(int i = 0; i < getKeySize(); ++i) {
            if(!setFinger(i, nodeInfo)) return false;
        }
        return true;
    }

    /**
     * @brief Join an existing chord system.
     *
     * @param gateway   Socket address of the gateway node that will be used to join the system
     * @param moveKeys  Whatever operations the upper layer may want to execute just before ending the Join
     */
    public boolean join(InetSocketAddress gateway, ProtocolTask<Boolean> moveKeys) {
        return new JoinProtocol(this, gateway, moveKeys).invoke();
    }

    public boolean leave(ProtocolTask<Boolean> moveKeys) {
        return new LeaveProtocol(this, moveKeys).invoke();
    }

    public static long distance(Chord.Key a, Chord.Key b) {
        assert(a.chord.getMod() == b.chord.getMod());
        long MOD = a.chord.getMod();
        return (b.subtract(a) + MOD) % MOD;
    }
}
