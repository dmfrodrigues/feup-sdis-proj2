package sdis.Modules.Chord;

import org.junit.Test;
import sdis.Peer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestChordJoin {
    public long getExpectedSuccessor(List<Long> listOfPeers, long key, long MOD){
        key = ((key % MOD) + MOD) % MOD;

        int i = Collections.binarySearch(listOfPeers, key);
        i = (i < 0 ? -(i+1) : i);
        if(i == listOfPeers.size()) i = 0;
        return listOfPeers.get(i);
    }

    @Test(timeout=1000)
    public void peer1_small_checkFingers() throws Exception {
        Peer peer1 = new Peer(8, 0, InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peer1.join());

        Chord chord1 = peer1.getChord();

        assertEquals(0, chord1.getPredecessorInfo().key.toLong());
        assertEquals(0, chord1.getSuccessorInfo().key.toLong());
        assertEquals(0, chord1.getFingerInfo(0).key.toLong());
        assertEquals(0, chord1.getFingerInfo(1).key.toLong());
        assertEquals(0, chord1.getFingerInfo(2).key.toLong());
        assertEquals(0, chord1.getFingerInfo(3).key.toLong());
        assertEquals(0, chord1.getFingerInfo(4).key.toLong());
        assertEquals(0, chord1.getFingerInfo(5).key.toLong());
        assertEquals(0, chord1.getFingerInfo(6).key.toLong());
        assertEquals(0, chord1.getFingerInfo(7).key.toLong());

        assertTrue(peer1.leave());
    }

    @Test(timeout=2000)
    public void peer1_small() throws Exception {
        Peer peer1 = new Peer(8, 0, InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peer1.join());

        Chord chord = peer1.getChord();

        assertEquals(0, chord.findSuccessor(chord.newKey(  0)).key.toLong());
        assertEquals(0, chord.findSuccessor(chord.newKey(  1)).key.toLong());
        assertEquals(0, chord.findSuccessor(chord.newKey(  5)).key.toLong());
        assertEquals(0, chord.findSuccessor(chord.newKey( 10)).key.toLong());
        assertEquals(0, chord.findSuccessor(chord.newKey( 50)).key.toLong());
        assertEquals(0, chord.findSuccessor(chord.newKey(100)).key.toLong());
        assertEquals(0, chord.findSuccessor(chord.newKey(200)).key.toLong());
        assertEquals(0, chord.findSuccessor(chord.newKey(255)).key.toLong());

        assertEquals(0, chord.findPredecessor(chord.newKey(  0)).key.toLong());
        assertEquals(0, chord.findPredecessor(chord.newKey(  1)).key.toLong());
        assertEquals(0, chord.findPredecessor(chord.newKey(  5)).key.toLong());
        assertEquals(0, chord.findPredecessor(chord.newKey( 10)).key.toLong());
        assertEquals(0, chord.findPredecessor(chord.newKey( 50)).key.toLong());
        assertEquals(0, chord.findPredecessor(chord.newKey(100)).key.toLong());
        assertEquals(0, chord.findPredecessor(chord.newKey(200)).key.toLong());
        assertEquals(0, chord.findPredecessor(chord.newKey(255)).key.toLong());

        assertTrue(peer1.leave());
    }

    @Test(timeout=8000)
    public void peer1_large() throws Exception {
        Peer peer1 = new Peer(8, 0, InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peer1.join());

        Chord chord1 = peer1.getChord();

        List<Long> peers = new ArrayList<>(){{
            add(0L);
        }};

        for(long key = 0; key < peer1.getChord().getMod(); ++key){
            assertEquals(
                getExpectedSuccessor(peers, key, 1L<<8),
                chord1.findSuccessor(chord1.newKey(key)).key.toLong()
            );
        }

        assertTrue(peer1.leave());
    }

    @Test(timeout=4000)
    public void peer2_small_checkFingers() throws Exception {
        Peer peer1 = new Peer(8, 0, InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peer1.join());
        InetSocketAddress addressPeer1 = peer1.getSocketAddress();

        Peer peer2 = new Peer(8, 10, InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peer2.join(addressPeer1));

        Chord chord1 = peer1.getChord();
        Chord chord2 = peer2.getChord();

        assertEquals(10, chord1.getPredecessorInfo().key.toLong());
        assertEquals(0, chord2.getPredecessorInfo().key.toLong());

        assertEquals(0, chord2.getFingerInfo(0).key.toLong());
        assertEquals(0, chord2.getFingerInfo(1).key.toLong());
        assertEquals(0, chord2.getFingerInfo(2).key.toLong());
        assertEquals(0, chord2.getFingerInfo(3).key.toLong());
        assertEquals(0, chord2.getFingerInfo(4).key.toLong());
        assertEquals(0, chord2.getFingerInfo(5).key.toLong());
        assertEquals(0, chord2.getFingerInfo(6).key.toLong());
        assertEquals(0, chord2.getFingerInfo(7).key.toLong());

        assertEquals(10, chord1.getFingerInfo(0).key.toLong());
        assertEquals(10, chord1.getFingerInfo(1).key.toLong());
        assertEquals(10, chord1.getFingerInfo(2).key.toLong());
        assertEquals(10, chord1.getFingerInfo(3).key.toLong());
        assertEquals( 0, chord1.getFingerInfo(4).key.toLong());
        assertEquals( 0, chord1.getFingerInfo(5).key.toLong());
        assertEquals( 0, chord1.getFingerInfo(6).key.toLong());
        assertEquals( 0, chord1.getFingerInfo(7).key.toLong());

        assertEquals(10, chord1.getSuccessorInfo().key.toLong());
        assertEquals(0, chord2.getSuccessorInfo().key.toLong());

        assertTrue(peer1.leave());
        assertTrue(peer2.leave());
    }

    @Test(timeout=8000)
    public void peer2_small() throws Exception {
        Peer peer1 = new Peer(8, 0, InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peer1.join());
        InetSocketAddress addressPeer1 = peer1.getSocketAddress();

        Peer peer2 = new Peer(8, 10, InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peer2.join(addressPeer1));

        Chord chord1 = peer1.getChord();

        assertEquals(10, chord1.findPredecessor(chord1.newKey(  0)).key.toLong());
        assertEquals( 0, chord1.findPredecessor(chord1.newKey(  1)).key.toLong());
        assertEquals( 0, chord1.findPredecessor(chord1.newKey(  5)).key.toLong());
        assertEquals( 0, chord1.findPredecessor(chord1.newKey( 10)).key.toLong());
        assertEquals(10, chord1.findPredecessor(chord1.newKey( 50)).key.toLong());
        assertEquals(10, chord1.findPredecessor(chord1.newKey(100)).key.toLong());
        assertEquals(10, chord1.findPredecessor(chord1.newKey(200)).key.toLong());
        assertEquals(10, chord1.findPredecessor(chord1.newKey(255)).key.toLong());

        assertEquals( 0, chord1.findSuccessor(chord1.newKey(  0)).key.toLong());
        assertEquals(10, chord1.findSuccessor(chord1.newKey(  1)).key.toLong());
        assertEquals(10, chord1.findSuccessor(chord1.newKey(  5)).key.toLong());
        assertEquals(10, chord1.findSuccessor(chord1.newKey( 10)).key.toLong());
        assertEquals( 0, chord1.findSuccessor(chord1.newKey( 50)).key.toLong());
        assertEquals( 0, chord1.findSuccessor(chord1.newKey(100)).key.toLong());
        assertEquals( 0, chord1.findSuccessor(chord1.newKey(200)).key.toLong());
        assertEquals( 0, chord1.findSuccessor(chord1.newKey(255)).key.toLong());

        Chord chord2 = peer2.getChord();

        assertEquals(10, chord1.findPredecessor(chord1.newKey(  0)).key.toLong());
        assertEquals( 0, chord1.findPredecessor(chord1.newKey(  1)).key.toLong());
        assertEquals( 0, chord1.findPredecessor(chord1.newKey(  5)).key.toLong());
        assertEquals( 0, chord1.findPredecessor(chord1.newKey( 10)).key.toLong());
        assertEquals(10, chord1.findPredecessor(chord1.newKey( 50)).key.toLong());
        assertEquals(10, chord1.findPredecessor(chord1.newKey(100)).key.toLong());
        assertEquals(10, chord1.findPredecessor(chord1.newKey(200)).key.toLong());
        assertEquals(10, chord1.findPredecessor(chord1.newKey(255)).key.toLong());

        assertEquals( 0, chord2.findSuccessor(chord2.newKey(  0)).key.toLong());
        assertEquals(10, chord2.findSuccessor(chord2.newKey(  1)).key.toLong());
        assertEquals(10, chord2.findSuccessor(chord2.newKey(  5)).key.toLong());
        assertEquals(10, chord2.findSuccessor(chord2.newKey( 10)).key.toLong());
        assertEquals( 0, chord2.findSuccessor(chord2.newKey( 50)).key.toLong());
        assertEquals( 0, chord2.findSuccessor(chord2.newKey(100)).key.toLong());
        assertEquals( 0, chord2.findSuccessor(chord2.newKey(200)).key.toLong());
        assertEquals( 0, chord2.findSuccessor(chord2.newKey(255)).key.toLong());

        assertTrue(peer1.leave());
        assertTrue(peer2.leave());
    }

    @Test(timeout=40000)
    public void peer2_large() throws Exception {
        Peer peer1 = new Peer(8, 0, InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peer1.join());
        Chord chord1 = peer1.getChord();
        InetSocketAddress addressPeer1 = peer1.getSocketAddress();

        Peer peer2 = new Peer(8, 10, InetAddress.getByName("localhost"), Paths.get("bin"));
        Chord chord2 = peer2.getChord();
        assertTrue(peer2.join(addressPeer1));

        List<Long> peers = new ArrayList<>(){{
            add(0L);
            add(10L);
        }};

        for(long key = 0; key < peer1.getChord().getMod(); ++key){
            assertEquals(getExpectedSuccessor(peers, key, 1L<<8), chord1.findSuccessor(chord1.newKey(key)).key.toLong());
            assertEquals(getExpectedSuccessor(peers, key, 1L<<8), chord2.findSuccessor(chord2.newKey(key)).key.toLong());
        }

        assertTrue(peer1.leave());
        assertTrue(peer2.leave());
    }

    @Test(timeout=20000)
    public void peer2_larger() throws Exception {
        int keySize = 6;
        long MOD = 1L << keySize;
        Peer peer1 = new Peer(keySize, 0, InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peer1.join());
        Chord chord1 = peer1.getChord();
        InetSocketAddress addressPeer1 = peer1.getSocketAddress();

        Peer peer2 = new Peer(keySize, 16, InetAddress.getByName("localhost"), Paths.get("bin"));
        Chord chord2 = peer2.getChord();
        assertTrue(peer2.join(addressPeer1));

        List<Long> peers = new ArrayList<>(){{
            add(0L);
            add(16L);
        }};

        for(int i = 0; i < keySize; ++i){
            assertEquals(getExpectedSuccessor(peers, chord1.getNodeInfo().key.toLong() + (1L << i), MOD), chord1.getFingerInfo(i).key.toLong());
            assertEquals(getExpectedSuccessor(peers, chord2.getNodeInfo().key.toLong() + (1L << i), MOD), chord2.getFingerInfo(i).key.toLong());
        }

        for(long key = 0; key < peer1.getChord().getMod(); ++key){
            assertEquals(getExpectedSuccessor(peers, key, MOD), chord1.findSuccessor(chord1.newKey(key)).key.toLong());
            assertEquals(getExpectedSuccessor(peers, key, MOD), chord2.findSuccessor(chord2.newKey(key)).key.toLong());
        }

        assertTrue(peer1.leave());
        assertTrue(peer2.leave());
    }

    @Test(timeout=20000)
    public void peer3_large() throws Exception {
        int keySize = 10;
        long MOD = (1L << keySize);

        Peer peer1 = new Peer(keySize, 0, InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peer1.join());
        Chord chord1 = peer1.getChord();

        InetSocketAddress addressPeer1 = peer1.getSocketAddress();

        Peer peer2 = new Peer(keySize, 100, InetAddress.getByName("localhost"), Paths.get("bin"));
        Chord chord2 = peer2.getChord();
        assertTrue(peer2.join(addressPeer1));

        List<Long> peers = new ArrayList<>(){{ add(0L); add(100L); }};
        for(int i = 0; i < keySize; ++i){
            assertEquals(getExpectedSuccessor(peers, chord1.getNodeInfo().key.toLong() + (1L << i), MOD), chord1.getFingerInfo(i).key.toLong());
            assertEquals(getExpectedSuccessor(peers, chord2.getNodeInfo().key.toLong() + (1L << i), MOD), chord2.getFingerInfo(i).key.toLong());
        }

        Peer peer3 = new Peer(keySize, 356, InetAddress.getByName("localhost"), Paths.get("bin"));
        Chord chord3 = peer3.getChord();
        assertTrue(peer3.join(addressPeer1));

        peers = new ArrayList<>(){{ add(0L); add(100L); add(356L); }};

        assertEquals(356, chord1.getPredecessorInfo().key.toLong());
        assertEquals(  0, chord2.getPredecessorInfo().key.toLong());
        assertEquals(100, chord3.getPredecessorInfo().key.toLong());
        
        for(int i = 0; i < keySize; ++i){
            assertEquals(getExpectedSuccessor(peers, chord1.getNodeInfo().key.toLong() + (1L << i), MOD), chord1.getFingerInfo(i).key.toLong());
            assertEquals(getExpectedSuccessor(peers, chord2.getNodeInfo().key.toLong() + (1L << i), MOD), chord2.getFingerInfo(i).key.toLong());
            assertEquals(getExpectedSuccessor(peers, chord3.getNodeInfo().key.toLong() + (1L << i), MOD), chord3.getFingerInfo(i).key.toLong());
        }

        for(long key = 0; key < peer1.getChord().getMod(); key += 10){
            assertEquals(getExpectedSuccessor(peers, key, MOD), chord1.findSuccessor(chord1.newKey(key)).key.toLong());
            assertEquals(getExpectedSuccessor(peers, key, MOD), chord2.findSuccessor(chord2.newKey(key)).key.toLong());
            assertEquals(getExpectedSuccessor(peers, key, MOD), chord3.findSuccessor(chord3.newKey(key)).key.toLong());
        }

        assertTrue(peer1.leave());
        assertTrue(peer2.leave());
        assertTrue(peer3.leave());
    }

    @Test(timeout=40000)
    public void peer10_large() throws Exception {
        int keySize = 10;
        long MOD = (1L << keySize);

        long[] ids = {
                237, 716, 727, 758, 806,
                55, 0, 100, 356, 662
        };
        Set<Long> set = new HashSet<>();
        for (Long l : ids) set.add(l);
        assertEquals(ids.length, set.size());
        int[] addressIndexes = new int[]{
                0, 0, 1, 2, 2,
                3, 0, 2, 5, 4
        };

        List<Long> idsSorted = new ArrayList<>();
        List<Peer> peers = new ArrayList<>();
        for (int i = 0; i < ids.length; ++i) {
            Peer peer = new Peer(keySize, ids[i], InetAddress.getByName("localhost"), Paths.get("bin"));
            peers.add(peer);
            idsSorted.add(ids[i]);
            Collections.sort(idsSorted);
            if (i == 0) {
                assertTrue(peer.join());
            } else {
                InetSocketAddress gateway = peers.get(addressIndexes[i]).getSocketAddress();
                assertTrue(peer.join(gateway));
            }

            for (Peer p : peers) {
                Chord chord = p.getChord();
                for (int j = 0; j < keySize; ++j) {
                    assertEquals(getExpectedSuccessor(idsSorted, chord.getNodeInfo().key.toLong() + (1L << j), MOD), chord.getFingerInfo(j).key.toLong());
                }
            }
        }

        for (Peer p : peers)
            assertTrue(p.leave());
    }

    @Test(timeout=300000)
    public void peer20_large() throws Exception {
        int keySize = 10;
        long MOD = (1L << keySize);

        long[] ids = {
                172, 716, 982, 540, 662,
                284, 806, 185, 623, 427,
                237, 55, 758, 785, 863,
                727, 946, 203, 557, 308
        };
        Set<Long> set = new HashSet<>();
        for (Long l : ids) set.add(l);
        assertEquals(ids.length, set.size());
        int[] addressIndexes = new int[]{
                0, 0, 1, 2, 2,
                3, 0, 2, 5, 4,
                2, 8, 6, 1, 13,
                0, 13, 5, 7, 11
        };

        Peer[] peers = new Peer[ids.length];
        for (int i = 0; i < ids.length; ++i) {
            peers[i] = new Peer(keySize, ids[i], InetAddress.getByName("localhost"), Paths.get("bin"));
            if (i == 0) {
                assertTrue(peers[i].join());
            } else {
                InetSocketAddress gateway = peers[addressIndexes[i]].getSocketAddress();
                assertTrue(peers[i].join(gateway));
            }
        }

        List<Long> idsSorted = new ArrayList<>();
        for(Long l: ids) idsSorted.add(l);
        Collections.sort(idsSorted);

        for (Peer peer : peers) {
            for (int i = 0; i < keySize; ++i) {
                Chord chord = peer.getChord();
                assertEquals(getExpectedSuccessor(idsSorted, chord.getNodeInfo().key.toLong() + (1L << i), MOD), chord.getFingerInfo(i).key.toLong());
            }
        }

        int increment = 60;
        long startTime = System.nanoTime();
        for (Peer peer : peers) {
            for (long key = 0; key < MOD; key += increment) {
                Chord chord = peer.getChord();
                assertEquals(getExpectedSuccessor(idsSorted, key, MOD), chord.findSuccessor(chord.newKey(key)).key.toLong());
            }
        }
        long endTime = System.nanoTime();
        float Dt = (endTime-startTime)/1000000000.0f;
        System.err.println("Took " + Dt + " seconds (avg. per operation " + 1000.0f * Dt/(peers.length * MOD/increment) + "ms)");

        for (Peer peer : peers) {
            assertTrue(peer.leave());
        }
    }
}
