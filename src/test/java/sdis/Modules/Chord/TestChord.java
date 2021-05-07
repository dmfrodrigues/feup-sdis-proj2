package sdis.Modules.Chord;

import org.junit.Test;
import sdis.Peer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

public class TestChord {
    public void testSuccessor(Chord chord, long[] listOfPeers, long key) throws ExecutionException, InterruptedException {
        assertNotNull(chord);

        int i = Arrays.binarySearch(listOfPeers, key);
        i = (i < 0 ? -(i+1) : i);
        if(i == listOfPeers.length) i = 0;
        long expected = listOfPeers[i];

        Chord.NodeInfo successor = chord.getSuccessor(new Chord.Key(key)).get();
        assertNotNull(successor);
        long actual = successor.key.toLong();

        assertEquals(expected, actual);
    }

    @Test
    public void testChord_1peer_small() throws Exception {
        Chord.setKeySize(8);

        Peer peer1 = new Peer(new Chord.Key(0), InetAddress.getByName("localhost"));
        peer1.join();

        Chord chord = peer1.getChord();

        assertEquals(chord.getSuccessor(new Chord.Key(0)).get().key.toLong(), 0);
        assertEquals(chord.getSuccessor(new Chord.Key(1)).get().key.toLong(), 0);
        assertEquals(chord.getSuccessor(new Chord.Key(5)).get().key.toLong(), 0);
        assertEquals(chord.getSuccessor(new Chord.Key(10)).get().key.toLong(), 0);
        assertEquals(chord.getSuccessor(new Chord.Key(50)).get().key.toLong(), 0);
        assertEquals(chord.getSuccessor(new Chord.Key(100)).get().key.toLong(), 0);
        assertEquals(chord.getSuccessor(new Chord.Key(200)).get().key.toLong(), 0);
        assertEquals(chord.getSuccessor(new Chord.Key(255)).get().key.toLong(), 0);
    }

    @Test
    public void testChord_1peer_large() throws Exception {
        Chord.setKeySize(8);

        Peer peer1 = new Peer(new Chord.Key(0), InetAddress.getByName("localhost"));
        peer1.join();

        long[] peers = new long[]{0};

        for(long key = 0; key < Chord.getMod(); ++key){
            testSuccessor(peer1.getChord(), peers, key);
        }
    }

    @Test
    public void testChord_2peer_small() throws Exception {
        Chord.setKeySize(8);

        Peer peer1 = new Peer(new Chord.Key(0), InetAddress.getByName("localhost"));
        peer1.join();
        InetSocketAddress addressPeer1 = peer1.getSocketAddress();

        Peer peer2 = new Peer(new Chord.Key(10), InetAddress.getByName("localhost"));
        peer2.join(addressPeer1);

        Chord chord = peer1.getChord();

        assertEquals(chord.getSuccessor(new Chord.Key(0)).get().key.toLong(), 0);
        assertEquals(chord.getSuccessor(new Chord.Key(1)).get().key.toLong(), 0);
        assertEquals(chord.getSuccessor(new Chord.Key(5)).get().key.toLong(), 0);
        assertEquals(chord.getSuccessor(new Chord.Key(10)).get().key.toLong(), 0);
        assertEquals(chord.getSuccessor(new Chord.Key(50)).get().key.toLong(), 0);
        assertEquals(chord.getSuccessor(new Chord.Key(100)).get().key.toLong(), 0);
        assertEquals(chord.getSuccessor(new Chord.Key(200)).get().key.toLong(), 0);
        assertEquals(chord.getSuccessor(new Chord.Key(255)).get().key.toLong(), 0);

        chord = peer2.getChord();

        assertEquals(chord.getSuccessor(new Chord.Key(0)).get().key.toLong(), 0);
        assertEquals(chord.getSuccessor(new Chord.Key(1)).get().key.toLong(), 0);
        assertEquals(chord.getSuccessor(new Chord.Key(5)).get().key.toLong(), 0);
        assertEquals(chord.getSuccessor(new Chord.Key(10)).get().key.toLong(), 0);
        assertEquals(chord.getSuccessor(new Chord.Key(50)).get().key.toLong(), 0);
        assertEquals(chord.getSuccessor(new Chord.Key(100)).get().key.toLong(), 0);
        assertEquals(chord.getSuccessor(new Chord.Key(200)).get().key.toLong(), 0);
        assertEquals(chord.getSuccessor(new Chord.Key(255)).get().key.toLong(), 0);
    }

    @Test
    public void testChord_2peer_large() throws Exception {
        Chord.setKeySize(8);

        Peer peer1 = new Peer(new Chord.Key(0), InetAddress.getByName("localhost"));
        peer1.join();
        InetSocketAddress addressPeer1 = peer1.getSocketAddress();

        Peer peer2 = new Peer(new Chord.Key(10), InetAddress.getByName("localhost"));
        peer2.join(addressPeer1);

        long[] peers = new long[]{0, 10};

        for(long key = 0; key < Chord.getMod(); ++key){
            testSuccessor(peer1.getChord(), peers, key);
            testSuccessor(peer2.getChord(), peers, key);
        }
    }
}
