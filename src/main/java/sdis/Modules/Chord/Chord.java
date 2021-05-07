package sdis.Modules.Chord;

import sdis.Modules.Chord.Messages.ChordMessage;
import sdis.Modules.ProtocolSupplier;

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

    public static class NodeInfo {
        public final Chord.Key key;
        public final InetSocketAddress address;

        public NodeInfo(Chord.Key key, InetSocketAddress address){
            this.key = key;
            this.address = address;
        }

        public NodeInfo(byte[] data) {
            String dataString = new String(data);
            String[] splitString = dataString.split(" ");
            key = new Chord.Key(Long.parseLong(splitString[0]));
            String[] splitAddress = splitString[1].split(":");
            address = new InetSocketAddress(splitAddress[0], Integer.parseInt(splitAddress[1]));
        }

        public String toString() {
            return key + " " + address.getAddress().getHostAddress() + ":" + address.getPort();
        }

        public boolean equals(NodeInfo obj){
            return (key == obj.key && address.equals(obj.address));
        }
    }

    private static int m = 30;

    private final InetSocketAddress socketAddress;
    private final Executor executor;
    private final Chord.Key key;
    private final NodeInfo[] fingers;
    private NodeInfo predecessor;

    public Chord(InetSocketAddress socketAddress, Executor executor, Chord.Key key){
        this.socketAddress = socketAddress;
        this.executor = executor;
        this.key = key;
        fingers = new NodeInfo[m];
    }

    public Executor getExecutor() {
        return executor;
    }

    public Chord.Key getKey() {
        return key;
    }

    public NodeInfo getFinger(int i){
        return fingers[i];
    }

    public void setFinger(int i, NodeInfo peer){
        fingers[i] = peer;
    }

    public NodeInfo getPredecessor(){
        return predecessor;
    }

    public void setPredecessor(NodeInfo peer){
        predecessor = peer;
    }

    public NodeInfo getSuccessor() {
        return getFinger(0);
    }

    public CompletableFuture<NodeInfo> getSuccessor(Chord.Key key){
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

    public InetSocketAddress getSocketAddress(){
        return socketAddress;
    }

    public NodeInfo getPeerInfo() {
        return new NodeInfo(key, getSocketAddress());
    }

    /**
     * @brief Create a new chord system.
     *
     * @return Future that will resolve when join is complete.
     */
    public CompletableFuture<Void> join(){
        return CompletableFuture.runAsync(() -> {
            NodeInfo nodeInfo = getPeerInfo();
            setPredecessor(nodeInfo);
            for(int i = 0; i < getKeySize(); ++i){
                setFinger(i, nodeInfo);
            }
        }, getExecutor());
    }

    /**
     * @brief Join an existing chord system.
     *
     * @param gateway   Socket address of the gateway node that will be used to join the system
     * @param moveKeys  Whatever operations the upper layer may want to execute just before ending the Join
     * @return  Future of the completion of the join procedure
     */
    public CompletableFuture<Void> join(InetSocketAddress gateway, ProtocolSupplier<Void> moveKeys) {
        JoinProtocol joinProtocol = new JoinProtocol(this, gateway, moveKeys);
        return CompletableFuture.supplyAsync(joinProtocol, getExecutor());
    }

    public Socket send(InetSocketAddress to, ChordMessage m) throws IOException {
        Socket socket = new Socket(to.getAddress(), to.getPort());
        OutputStream os = socket.getOutputStream();
        os.write(m.toString().getBytes());
        os.flush();
        return socket;
    }

    public Socket send(Chord.NodeInfo to, ChordMessage m) throws IOException {
        return send(to.address, m);
    }

    public long distance(Chord.Key a, Chord.Key b) {
        long MOD = 1L << getKeySize();
        return (b.subtract(a) + MOD) % MOD;
    }
}
