package sdis.Modules.Chord;

import sdis.Modules.Chord.Messages.HelloMessage;
import sdis.Modules.ProtocolTask;
import sdis.Utils.Utils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

        public static NodeInfo fromString(Chord chord, String s) {
            String[] splitString = s.split(" ");
            Chord.Key key = chord.newKey(Long.parseLong(splitString[0]));
            String[] splitAddress = splitString[1].split(":");
            InetSocketAddress address = new InetSocketAddress(splitAddress[0], Integer.parseInt(splitAddress[1]));
            return new Chord.NodeInfo(key, address);
        }

        public void copy(NodeInfo nodeInfo){
            key     = nodeInfo.key;
            address = nodeInfo.address;
        }

        public SocketChannel createSocket() throws IOException {
            return Utils.createSocket(key.chord.sslContext, address);
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

    public static class NodeConn {
        public final NodeInfo nodeInfo;
        public final SocketChannel socket;

        public NodeConn(NodeInfo nodeInfo, SocketChannel socket){
            this.nodeInfo = nodeInfo;
            this.socket = socket;
        }
    }

    public static final int SUCCESSOR_LIST_SIZE = 5;
    public static final int FIXES_DELTA_MILLIS = 10000;

    private final int keySize;

    private final SSLContext sslContext;
    private final InetSocketAddress socketAddress;
    private final Chord.Key key;
    private final NodeInfo[] fingers;
    private final NodeInfo predecessor;
    private final TreeSet<NodeInfo> successors;

    private final ScheduledExecutorService executorOfFixes = Executors.newSingleThreadScheduledExecutor();

    public Chord(SSLContext sslContext, InetSocketAddress socketAddress, int keySize, long key){
        this.sslContext = sslContext;
        this.socketAddress = socketAddress;
        this.keySize = keySize;
        this.key = newKey(key);
        fingers = new NodeInfo[this.keySize];
        predecessor = new NodeInfo();
        successors = new TreeSet<>((NodeInfo a, NodeInfo b) -> {
            long diff = Chord.distance(this.key, a.key) - Chord.distance(this.key, b.key);
            return (diff < 0 ? -1 : (diff > 0 ? +1 : 0));
        });
    }

    public SSLContext getSSLContext() {
        return sslContext;
    }

    public void scheduleFixes(){
        executorOfFixes.scheduleAtFixedRate(this::fix, FIXES_DELTA_MILLIS/2, FIXES_DELTA_MILLIS, TimeUnit.MILLISECONDS);
    }

    public void killFixes() {
        executorOfFixes.shutdown();
    }

    public Chord.Key newKey(long k){
        return new Chord.Key(this, k);
    }

    public NodeInfo getFingerRaw(int i){
        synchronized(fingers) {
            return fingers[i];
        }
    }

    public NodeInfo getFingerInfo(int i) {
        try {
            synchronized (fingers) {
                SocketChannel socket = fingers[i].createSocket();
                HelloMessage helloMessage = new HelloMessage();
                helloMessage.sendTo(this, socket);
                return fingers[i];
            }
        } catch(ConnectException e) {
            System.err.println("Node " + key + ": Failed to find finger " + i + ", recalculating");
            NodeInfo finger = findSuccessor(key.add(1L << i));
            if(finger == null) {
                System.err.println("Node " + key + ": Failed to recalculate finger " + i);
                throw new CompletionException(e);
            }
            setFinger(i, finger);
            System.err.println("Node " + key + ": Recalculated finger " + i + " and found it is " + finger.key);
            return finger;
        } catch (IOException | InterruptedException e) {
            throw new CompletionException(e);
        }
    }

    public boolean setFinger(int i, NodeInfo peer){
        synchronized(fingers) {
            fingers[i] = peer;
        }
        return true;
    }

    public NodeInfo getPredecessorInfo(){
        try {
            try {
                synchronized (predecessor) {
                    SocketChannel socket = predecessor.createSocket();
                    HelloMessage helloMessage = new HelloMessage();
                    helloMessage.sendTo(this, socket);
                    return predecessor;
                }
            } catch (ConnectException e) {
                synchronized (predecessor) {
                    System.err.println("Node " + key + ": Failed to find predecessor (" + predecessor.key + "), recalculating");
                }
                NodeInfo p = findPredecessor(key);
                if (p == null) {
                    System.err.println("Node " + key + ": Failed to recalculate predecessor");
                    throw new CompletionException(e);
                }
                setPredecessor(p);
                System.err.println("Node " + key + ": Recalculated predecessor and found it is " + p.key);
                return predecessor;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new CompletionException(e);
        }
    }

    public NodeConn getPredecessor(){
        try {
            try {
                synchronized (predecessor) {
                    return new NodeConn(new NodeInfo(predecessor), predecessor.createSocket());
                }
            } catch (ConnectException e) {
                synchronized (predecessor) {
                    System.err.println("Node " + key + ": Failed to find predecessor (" + predecessor.key + "), recalculating");
                }
                NodeInfo p = findPredecessor(key);
                if (p == null) {
                    System.err.println("Node " + key + ": Failed to recalculate predecessor");
                    throw new CompletionException(e);
                }
                setPredecessor(p);
                System.err.println("Node " + key + ": Recalculated predecessor and found it is " + p.key);
                return new NodeConn(new NodeInfo(predecessor), predecessor.createSocket());
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new CompletionException(e);
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

    public void addSuccessor(NodeInfo s) {
        synchronized (successors) {
            successors.add(s);
            Iterator<NodeInfo> it = successors.iterator();
            while (it.hasNext()) {
                NodeInfo t = it.next();
                try {
                    new HelloMessage().sendTo(this, t.createSocket());
                } catch (ConnectException e) {
                    System.err.println("Node " + key + ": While inserting a new successor " + s.key + ", noticed " + t.key + " does not exist; purging");
                    it.remove();
                } catch (IOException | InterruptedException e) {
                    throw new CompletionException(e);
                }
            }
            while(successors.size() > SUCCESSOR_LIST_SIZE){
                successors.remove(successors.last());
            }
        }
    }

    public NodeInfo getSuccessorInfo() {
        synchronized(successors) {
            while (true) {
                if(successors.isEmpty()){
                    return getNodeInfo();
                }
                NodeInfo s = successors.first();
                try {
                    SocketChannel socket = s.createSocket();
                    HelloMessage helloMessage = new HelloMessage();
                    helloMessage.sendTo(this, socket);
                    return s;
                } catch (ConnectException e) {
                    System.err.println("Node " + key + ": Failed to use successor " + s.key + ", using next successor");
                    successors.remove(s);
                } catch (IOException | InterruptedException e) {
                    throw new CompletionException(e);
                }
            }
        }
    }

    public NodeConn getSuccessor() {
        synchronized(successors) {
            if(successors.isEmpty()) {
                try {
                    return new NodeConn(getNodeInfo(), getNodeInfo().createSocket());
                } catch (IOException e) {
                    throw new CompletionException(e);
                }
            }
            while (true) {
                NodeInfo s = successors.first();
                try {
                    return new NodeConn(s, s.createSocket());
                } catch (ConnectException e) {
                    System.err.println("Node " + key + ": Failed to use successor " + s.key + ", using next successor");
                    successors.remove(s);
                } catch (IOException e) {
                    throw new CompletionException(e);
                }
            }
        }
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
        for(int i = 0; i < keySize; ++i) {
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

    public boolean fix(){
        return new FixChordProtocol(this).invoke();
    }

    public static long distance(Chord.Key a, Chord.Key b) {
        assert(a.chord.getMod() == b.chord.getMod());
        long MOD = a.chord.getMod();
        return (b.subtract(a) + MOD) % MOD;
    }
}
