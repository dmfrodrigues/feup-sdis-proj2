package sdis.Modules.Chord;

import sdis.Modules.Chord.Messages.ChordMessage;
import sdis.Modules.ProtocolTask;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Objects;

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

        public String toString() {
            return key + " " + address.getAddress().getHostAddress() + ":" + address.getPort();
        }

        public boolean equals(NodeInfo obj){
            return (key.equals(obj.key) && address.equals(obj.address));
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

    public void setFinger(int i, NodeInfo peer){
        synchronized(fingers) {
            fingers[i] = peer;
        }
    }

    public NodeInfo getPredecessor(){
        synchronized(predecessor) {
            return new NodeInfo(predecessor);
        }
    }

    public void setPredecessor(NodeInfo peer){
        synchronized(predecessor) {
            predecessor.copy(peer);
        }
    }

    public NodeInfo getSuccessor() {
        return getFinger(0);
    }

    public NodeInfo getSuccessor(Chord.Key key){
        return new GetSuccessorProtocol(this, key).invoke();
    }

    public int getKeySize() {
        return keySize;
    }

    public long getMod(){
        return (1L << getKeySize());
    }

    public InetSocketAddress getSocketAddress(){
        return socketAddress;
    }

    public NodeInfo getNodeInfo() {
        return new NodeInfo(key, getSocketAddress());
    }

    /**
     * @brief Create a new chord system.
     */
    public void join(){
        NodeInfo nodeInfo = getNodeInfo();
        setPredecessor(nodeInfo);
        for(int i = 0; i < getKeySize(); ++i) {
            setFinger(i, nodeInfo);
        }
    }

    /**
     * @brief Join an existing chord system.
     *
     * @param gateway   Socket address of the gateway node that will be used to join the system
     * @param moveKeys  Whatever operations the upper layer may want to execute just before ending the Join
     */
    public void join(InetSocketAddress gateway, ProtocolTask<Void> moveKeys) {
        new JoinProtocol(this, gateway, moveKeys).invoke();
    }

    public void leave(ProtocolTask<Void> moveKeys) {
        new LeaveProtocol(this, moveKeys).invoke();
    }

    public Socket send(InetSocketAddress to, ChordMessage m) throws IOException {
        Socket socket = new Socket(to.getAddress(), to.getPort());
        OutputStream os = socket.getOutputStream();
        os.write(m.asByteArray());
        os.flush();
        return socket;
    }

    public Socket send(Chord.NodeInfo to, ChordMessage m) throws IOException {
        return send(to.address, m);
    }

    public static long distance(Chord.Key a, Chord.Key b) {
        assert(a.chord.getMod() == b.chord.getMod());
        long MOD = a.chord.getMod();
        return (b.subtract(a) + MOD) % MOD;
    }
}
