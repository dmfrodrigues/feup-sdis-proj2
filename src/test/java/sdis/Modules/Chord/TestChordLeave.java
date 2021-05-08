package sdis.Modules.Chord;

import org.junit.Test;
import sdis.Peer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;

import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;

public class TestChordLeave {
    public long getExpectedSuccessor(List<Long> listOfPeers, long key, long MOD){
        key = ((key % MOD) + MOD) % MOD;

        int i = Collections.binarySearch(listOfPeers, key);
        i = (i < 0 ? -(i+1) : i);
        if(i == listOfPeers.size()) i = 0;
        return listOfPeers.get(i);
    }

    @Test(timeout=10000)
    public void peer1() throws Exception {
        Peer peer1 = new Peer(8, 0, InetAddress.getByName("localhost"));
        peer1.join().get();

        peer1.leave().get();
    }

    @Test(timeout=10000)
    public void peer2_small_checkFingers() throws Exception {
        Peer peer1 = new Peer(8, 0, InetAddress.getByName("localhost"));
        peer1.join().get();
        InetSocketAddress addressPeer1 = peer1.getSocketAddress();

        Peer peer2 = new Peer(8, 10, InetAddress.getByName("localhost"));
        peer2.join(addressPeer1).get();
        peer2.leave().get();

        Chord chord1 = peer1.getChord();

        assertEquals(0, chord1.getPredecessor().key.toLong());

        assertEquals(0, chord1.getFinger(0).key.toLong());
        assertEquals(0, chord1.getFinger(1).key.toLong());
        assertEquals(0, chord1.getFinger(2).key.toLong());
        assertEquals(0, chord1.getFinger(3).key.toLong());
        assertEquals(0, chord1.getFinger(4).key.toLong());
        assertEquals(0, chord1.getFinger(5).key.toLong());
        assertEquals(0, chord1.getFinger(6).key.toLong());
        assertEquals(0, chord1.getFinger(7).key.toLong());
    }

}
