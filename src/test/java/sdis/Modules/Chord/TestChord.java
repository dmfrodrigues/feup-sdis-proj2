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

        Chord.NodeInfo successor = chord.getSuccessor(chord.newKey(key)).get();
        assertNotNull(successor);
        long actual = successor.key.toLong();

        assertEquals(expected, actual);
    }

    @Test
    public void testChord_1peer_small_checkFingers() throws Exception {
        Peer peer1 = new Peer(8, 0, InetAddress.getByName("localhost"));
        peer1.join().get();

        Chord chord1 = peer1.getChord();

        assertEquals(0, chord1.getFinger(0).key.toLong());
        assertEquals(0, chord1.getFinger(1).key.toLong());
        assertEquals(0, chord1.getFinger(2).key.toLong());
        assertEquals(0, chord1.getFinger(3).key.toLong());
        assertEquals(0, chord1.getFinger(4).key.toLong());
        assertEquals(0, chord1.getFinger(5).key.toLong());
        assertEquals(0, chord1.getFinger(6).key.toLong());
        assertEquals(0, chord1.getFinger(7).key.toLong());
    }

    @Test
    public void testChord_1peer_small() throws Exception {
        Peer peer1 = new Peer(8, 0, InetAddress.getByName("localhost"));
        peer1.join().get();

        Chord chord = peer1.getChord();

        assertEquals(0, chord.getSuccessor(chord.newKey(  0)).get().key.toLong());
        assertEquals(0, chord.getSuccessor(chord.newKey(  1)).get().key.toLong());
        assertEquals(0, chord.getSuccessor(chord.newKey(  5)).get().key.toLong());
        assertEquals(0, chord.getSuccessor(chord.newKey( 10)).get().key.toLong());
        assertEquals(0, chord.getSuccessor(chord.newKey( 50)).get().key.toLong());
        assertEquals(0, chord.getSuccessor(chord.newKey(100)).get().key.toLong());
        assertEquals(0, chord.getSuccessor(chord.newKey(200)).get().key.toLong());
        assertEquals(0, chord.getSuccessor(chord.newKey(255)).get().key.toLong());
    }

    @Test
    public void testChord_1peer_large() throws Exception {
        Peer peer1 = new Peer(8, 0, InetAddress.getByName("localhost"));
        peer1.join().get();

        long[] peers = new long[]{0};

        for(long key = 0; key < peer1.getChord().getMod(); ++key){
            testSuccessor(peer1.getChord(), peers, key);
        }
    }

    @Test
    public void testChord_2peer_small_checkFingers() throws Exception {
        Peer peer1 = new Peer(8, 0, InetAddress.getByName("localhost"));
        peer1.join().get();
        InetSocketAddress addressPeer1 = peer1.getSocketAddress();

        Peer peer2 = new Peer(8, 10, InetAddress.getByName("localhost"));
        peer2.join(addressPeer1).get();

        Chord chord1 = peer1.getChord();

        assertEquals(10, chord1.getPredecessor().key.toLong());

        assertEquals(10, chord1.getFinger(0).key.toLong());
        assertEquals(10, chord1.getFinger(1).key.toLong());
        assertEquals(10, chord1.getFinger(2).key.toLong());
        assertEquals(10, chord1.getFinger(3).key.toLong());
        assertEquals( 0, chord1.getFinger(4).key.toLong());
        assertEquals( 0, chord1.getFinger(5).key.toLong());
        assertEquals( 0, chord1.getFinger(6).key.toLong());
        assertEquals( 0, chord1.getFinger(7).key.toLong());

        Chord chord2 = peer2.getChord();

        assertEquals(0, chord2.getPredecessor().key.toLong());

        assertEquals(0, chord2.getFinger(0).key.toLong());
        assertEquals(0, chord2.getFinger(1).key.toLong());
        assertEquals(0, chord2.getFinger(2).key.toLong());
        assertEquals(0, chord2.getFinger(3).key.toLong());
        assertEquals(0, chord2.getFinger(4).key.toLong());
        assertEquals(0, chord2.getFinger(5).key.toLong());
        assertEquals(0, chord2.getFinger(6).key.toLong());
        assertEquals(0, chord2.getFinger(7).key.toLong());
    }

    @Test
    public void testChord_2peer_small() throws Exception {
        Peer peer1 = new Peer(8, 0, InetAddress.getByName("localhost"));
        peer1.join().get();
        InetSocketAddress addressPeer1 = peer1.getSocketAddress();

        Peer peer2 = new Peer(8, 10, InetAddress.getByName("localhost"));
        peer2.join(addressPeer1).get();

        Chord chord1 = peer1.getChord();

        assertEquals( 0, chord1.getSuccessor(chord1.newKey(  0)).get().key.toLong());
        assertEquals(10, chord1.getSuccessor(chord1.newKey(  1)).get().key.toLong());
        assertEquals(10, chord1.getSuccessor(chord1.newKey(  5)).get().key.toLong());
        assertEquals(10, chord1.getSuccessor(chord1.newKey( 10)).get().key.toLong());
        assertEquals( 0, chord1.getSuccessor(chord1.newKey( 50)).get().key.toLong());
        assertEquals( 0, chord1.getSuccessor(chord1.newKey(100)).get().key.toLong());
        assertEquals( 0, chord1.getSuccessor(chord1.newKey(200)).get().key.toLong());
        assertEquals( 0, chord1.getSuccessor(chord1.newKey(255)).get().key.toLong());

        Chord chord2 = peer2.getChord();

        assertEquals( 0, chord2.getSuccessor(chord2.newKey(  0)).get().key.toLong());
        assertEquals(10, chord2.getSuccessor(chord2.newKey(  1)).get().key.toLong());
        assertEquals(10, chord2.getSuccessor(chord2.newKey(  5)).get().key.toLong());
        assertEquals(10, chord2.getSuccessor(chord2.newKey( 10)).get().key.toLong());
        assertEquals( 0, chord2.getSuccessor(chord2.newKey( 50)).get().key.toLong());
        assertEquals( 0, chord2.getSuccessor(chord2.newKey(100)).get().key.toLong());
        assertEquals( 0, chord2.getSuccessor(chord2.newKey(200)).get().key.toLong());
        assertEquals( 0, chord2.getSuccessor(chord2.newKey(255)).get().key.toLong());
    }

    @Test
    public void testChord_2peer_large() throws Exception {
        Peer peer1 = new Peer(8, 0, InetAddress.getByName("localhost"));
        peer1.join().get();
        InetSocketAddress addressPeer1 = peer1.getSocketAddress();

        Peer peer2 = new Peer(8, 10, InetAddress.getByName("localhost"));
        peer2.join(addressPeer1).get();

        long[] peers = new long[]{0, 10};

        for(long key = 0; key < peer1.getChord().getMod(); ++key){
            testSuccessor(peer1.getChord(), peers, key);
            testSuccessor(peer2.getChord(), peers, key);
        }
    }
}
