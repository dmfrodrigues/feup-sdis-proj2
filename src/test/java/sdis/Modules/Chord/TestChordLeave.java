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

    @Test(timeout=10000)
    public void peer2_small() throws Exception {
        Peer peer1 = new Peer(8, 0, InetAddress.getByName("localhost"));
        Chord chord1 = peer1.getChord();
        peer1.join().get();
        InetSocketAddress addressPeer1 = peer1.getSocketAddress();

        Peer peer2 = new Peer(8, 10, InetAddress.getByName("localhost"));
        peer2.join(addressPeer1).get();
        peer2.leave().get();

        assertEquals( 0, chord1.getSuccessor(chord1.newKey(  0)).get().key.toLong());
        assertEquals( 0, chord1.getSuccessor(chord1.newKey(  1)).get().key.toLong());
        assertEquals( 0, chord1.getSuccessor(chord1.newKey(  5)).get().key.toLong());
        assertEquals( 0, chord1.getSuccessor(chord1.newKey( 10)).get().key.toLong());
        assertEquals( 0, chord1.getSuccessor(chord1.newKey( 50)).get().key.toLong());
        assertEquals( 0, chord1.getSuccessor(chord1.newKey(100)).get().key.toLong());
        assertEquals( 0, chord1.getSuccessor(chord1.newKey(200)).get().key.toLong());
        assertEquals( 0, chord1.getSuccessor(chord1.newKey(255)).get().key.toLong());
    }

    @Test(timeout=10000)
    public void peer2_large() throws Exception {
        Peer peer1 = new Peer(8, 0, InetAddress.getByName("localhost"));
        peer1.join().get();
        InetSocketAddress addressPeer1 = peer1.getSocketAddress();

        Peer peer2 = new Peer(8, 10, InetAddress.getByName("localhost"));
        Chord chord2 = peer2.getChord();
        peer2.join(addressPeer1).get();

        peer1.leave().get();

        List<Long> peers = new ArrayList<>(){{
            add(10L);
        }};

        for(long key = 0; key < peer1.getChord().getMod(); ++key){
            assertEquals(getExpectedSuccessor(peers, key, 1L<<8), chord2.getSuccessor(chord2.newKey(key)).get().key.toLong());
        }
    }

    @Test(timeout=10000)
    public void peer3_large() throws Exception {
        int keySize = 8;
        long MOD = (1L << keySize);

        Peer peer1 = new Peer(keySize, 0, InetAddress.getByName("localhost"));
        peer1.join().get();
        Chord chord1 = peer1.getChord();

        InetSocketAddress addressPeer1 = peer1.getSocketAddress();

        Peer peer2 = new Peer(keySize, 50, InetAddress.getByName("localhost"));
        Chord chord2 = peer2.getChord();
        peer2.join(addressPeer1).get();

        Peer peer3 = new Peer(keySize, 150, InetAddress.getByName("localhost"));
        Chord chord3 = peer3.getChord();
        peer3.join(addressPeer1).get();

        List<Long> peers = new ArrayList<>(){{ add(0L); add(50L); add(150L); }};
        for(int i = 0; i < keySize; ++i){
            assertEquals(getExpectedSuccessor(peers, chord1.getKey().toLong() + (1L << i), MOD), chord1.getFinger(i).key.toLong());
            assertEquals(getExpectedSuccessor(peers, chord2.getKey().toLong() + (1L << i), MOD), chord2.getFinger(i).key.toLong());
            assertEquals(getExpectedSuccessor(peers, chord3.getKey().toLong() + (1L << i), MOD), chord3.getFinger(i).key.toLong());
        }
        for(long key = 0; key < peer1.getChord().getMod(); ++key){
            assertEquals(getExpectedSuccessor(peers, key, MOD), chord1.getSuccessor(chord1.newKey(key)).get().key.toLong());
            assertEquals(getExpectedSuccessor(peers, key, MOD), chord2.getSuccessor(chord2.newKey(key)).get().key.toLong());
            assertEquals(getExpectedSuccessor(peers, key, MOD), chord3.getSuccessor(chord3.newKey(key)).get().key.toLong());
        }

        peer2.leave().get();

        peers = new ArrayList<>(){{ add(0L); add(150L); }};
        for(int i = 0; i < keySize; ++i){
            assertEquals(getExpectedSuccessor(peers, chord1.getKey().toLong() + (1L << i), MOD), chord1.getFinger(i).key.toLong());
            assertEquals(getExpectedSuccessor(peers, chord3.getKey().toLong() + (1L << i), MOD), chord3.getFinger(i).key.toLong());
        }
        for(long key = 0; key < peer1.getChord().getMod(); ++key){
            assertEquals(getExpectedSuccessor(peers, key, MOD), chord1.getSuccessor(chord1.newKey(key)).get().key.toLong());
            assertEquals(getExpectedSuccessor(peers, key, MOD), chord3.getSuccessor(chord3.newKey(key)).get().key.toLong());
        }

        peer1.leave().get();

        peers = new ArrayList<>(){{ add(150L); }};
        for(int i = 0; i < keySize; ++i){
            assertEquals(getExpectedSuccessor(peers, chord3.getKey().toLong() + (1L << i), MOD), chord3.getFinger(i).key.toLong());
        }
        for(long key = 0; key < peer1.getChord().getMod(); ++key){
            assertEquals(getExpectedSuccessor(peers, key, MOD), chord3.getSuccessor(chord3.newKey(key)).get().key.toLong());
        }
    }

    @Test(timeout=10000)
    public void peer20_large() throws Exception {
        int keySize = 10;
        long MOD = (1L << keySize);

        long[] ids = {
            172, 284, 540, //662, 716,
//            982, 806, 185, 623, 427,
//            237, 55, 758, 785, 863,
//            727, 946, 203, 557, 308,
        };
        Set<Long> set = new HashSet<>();
        for (Long l : ids) set.add(l);
        assertEquals(ids.length, set.size());
        int[] addressIndexes = new int[]{
                0, 0, 1, 2, 2,
                3, 0, 2, 5, 4,
                2, 8, 6, 1, 13,
                0, 13, 5, 7, 11,
        };
        int[] leaveIndexes = new int[]{
                0, 0, 0, 1, 3,
                3, 1, 0, 0, 8,
                5, 0, 7, 1, 13,
                8, 0, 7, 3, 7,
        };

        List<Peer> peers = new ArrayList<>();
        for (int i = 0; i < ids.length; ++i) {
            Peer peer = new Peer(keySize, ids[i], InetAddress.getByName("localhost"));
            peers.add(peer);
            if (i == 0) {
                peer.join().get();
            } else {
                InetSocketAddress gateway = peers.get(addressIndexes[i]).getSocketAddress();
                peer.join(gateway).get();
            }
        }

        List<Long> idsSorted = new ArrayList<>();
        for(Long l: ids) idsSorted.add(l);
        Collections.sort(idsSorted);

        int increment = 10;
        for (int i = ids.length - 1; i >= 0; i--) {
            Peer deletedPeer = peers.get(leaveIndexes[i]);
            idsSorted.remove(Collections.binarySearch(idsSorted, deletedPeer.getKey().toLong()));
            deletedPeer.leave().get();
            peers.remove(leaveIndexes[i]);

            System.out.println("Deleted peer " + deletedPeer.getKey() + ", only ones left are " + Arrays.toString(idsSorted.toArray()));
            for (Peer peer : peers) {
                Chord chord = peer.getChord();
                for (int j = 0; j < keySize; ++j){
                    assertEquals(getExpectedSuccessor(idsSorted, peer.getKey().toLong() + (1L << j), MOD), chord.getFinger(j).key.toLong());
                }
                for (long key = 0; key < MOD; key += increment) {
                    assertEquals(getExpectedSuccessor(idsSorted, key, MOD), chord.getSuccessor(chord.newKey(key)).get().key.toLong());
                }
            }
        }
    }
}
