package sdis.Modules.Main;

import org.junit.Test;
import sdis.Modules.DataStorage.DataStorage;
import sdis.Peer;
import sdis.Storage.ByteArrayChunkIterator;
import sdis.Storage.ChunkIterator;
import sdis.UUID;

import java.net.InetAddress;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class MainJoinLeaveTest {

    @Test(timeout=1000)
    public void join_2peers() throws Exception {
        int KEY_SIZE = 10;

        Peer peer1 = new Peer(KEY_SIZE, 0, InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peer1.join());
        DataStorage dataStorage1 = peer1.getDataStorage();

        Username username = new Username("user1");
        Password password = new Password("1234");
        Main.Path path = new Main.Path("mydata");
        byte[] data = "my data".getBytes();
        ChunkIterator chunkIterator = new ByteArrayChunkIterator(data, Main.CHUNK_SIZE);

        assertTrue(peer1.backup(username, password, path, 1, chunkIterator));

        Peer peer2 = new Peer(KEY_SIZE, 500, InetAddress.getByName("localhost"), Paths.get("bin"));
        peer2.join(peer1.getSocketAddress());
        DataStorage dataStorage2 = peer2.getDataStorage();

        assertNotNull(dataStorage1.get(new UUID("user1-0-1")));
        assertNotNull(dataStorage1.get(new UUID("user1-0-2")));
        assertNotNull(dataStorage1.get(new UUID("user1-0-3")));
        assertNotNull(dataStorage1.get(new UUID("user1-0-5")));
        assertNotNull(dataStorage1.get(new UUID("user1-0-6")));
        assertNotNull(dataStorage1.get(new UUID("user1-0-9")));
        assertNotNull(dataStorage1.get(new UUID("user1/mydata-0-0")));
        assertNotNull(dataStorage2.get(new UUID("user1-0-0")));
        assertNotNull(dataStorage2.get(new UUID("user1-0-4")));
        assertNotNull(dataStorage2.get(new UUID("user1-0-7")));
        assertNotNull(dataStorage2.get(new UUID("user1-0-8")));

        assertTrue(peer1.leave());
        assertTrue(peer2.leave());
    }

    @Test(timeout=1000)
    public void leave_2peers() throws Exception {
        int KEY_SIZE = 10;

        Peer peer1 = new Peer(KEY_SIZE, 0, InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peer1.join());
        DataStorage dataStorage1 = peer1.getDataStorage();

        Peer peer2 = new Peer(KEY_SIZE, 500, InetAddress.getByName("localhost"), Paths.get("bin"));
        peer2.join(peer1.getSocketAddress());
        DataStorage dataStorage2 = peer2.getDataStorage();

        Username username = new Username("user1");
        Password password = new Password("1234");
        Main.Path path = new Main.Path("mydata");
        byte[] data = "my data".getBytes();
        ChunkIterator chunkIterator = new ByteArrayChunkIterator(data, Main.CHUNK_SIZE);

        assertTrue(peer1.backup(username, password, path, 1, chunkIterator));

        assertNotNull(dataStorage1.get(new UUID("user1-0-1")));
        assertNotNull(dataStorage1.get(new UUID("user1-0-2")));
        assertNotNull(dataStorage1.get(new UUID("user1-0-3")));
        assertNotNull(dataStorage1.get(new UUID("user1-0-5")));
        assertNotNull(dataStorage1.get(new UUID("user1-0-6")));
        assertNotNull(dataStorage1.get(new UUID("user1-0-9")));
        assertNotNull(dataStorage1.get(new UUID("user1/mydata-0-0")));
        assertNotNull(dataStorage2.get(new UUID("user1-0-0")));
        assertNotNull(dataStorage2.get(new UUID("user1-0-4")));
        assertNotNull(dataStorage2.get(new UUID("user1-0-7")));
        assertNotNull(dataStorage2.get(new UUID("user1-0-8")));

        assertTrue(peer1.leave());

        assertNotNull(dataStorage2.get(new UUID("user1-0-0")));
        assertNotNull(dataStorage2.get(new UUID("user1-0-1")));
        assertNotNull(dataStorage2.get(new UUID("user1-0-2")));
        assertNotNull(dataStorage2.get(new UUID("user1-0-3")));
        assertNotNull(dataStorage2.get(new UUID("user1-0-4")));
        assertNotNull(dataStorage2.get(new UUID("user1-0-5")));
        assertNotNull(dataStorage2.get(new UUID("user1-0-6")));
        assertNotNull(dataStorage2.get(new UUID("user1-0-7")));
        assertNotNull(dataStorage2.get(new UUID("user1-0-8")));
        assertNotNull(dataStorage2.get(new UUID("user1-0-9")));
        assertNotNull(dataStorage2.get(new UUID("user1/mydata-0-0")));

        assertTrue(peer2.leave());
    }

    @Test(timeout=1000)
    public void die_3peers_simple() throws Exception {
        int KEY_SIZE = 10;

        Peer peer1 = new Peer(KEY_SIZE, 0, InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peer1.join());

        assertEquals(0, peer1.getChord().getSuccessorInfo().key.toLong());

        Peer peer2 = new Peer(KEY_SIZE, 100, InetAddress.getByName("localhost"), Paths.get("bin"));
        peer2.join(peer1.getSocketAddress());

        assertEquals(100, peer1.getChord().getSuccessorInfo().key.toLong());

        Peer peer3 = new Peer(KEY_SIZE, 200, InetAddress.getByName("localhost"), Paths.get("bin"));
        peer3.join(peer1.getSocketAddress());

        assertEquals(100, peer1.getChord().getSuccessorInfo().key.toLong());

        assertTrue(peer2.die());
        assertEquals(200, peer1.getChord().getSuccessorInfo().key.toLong());

        assertTrue(peer3.die());
        assertEquals(0, peer1.getChord().getSuccessorInfo().key.toLong());

        assertTrue(peer1.leave());
    }

    @Test(timeout=1000)
    public void die_2peers() throws Exception {
        int KEY_SIZE = 10;

        Peer peer1 = new Peer(KEY_SIZE, 0, InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peer1.join());

        Peer peer2 = new Peer(KEY_SIZE, 800, InetAddress.getByName("localhost"), Paths.get("bin"));
        peer2.join(peer1.getSocketAddress());

        assertEquals(  0, peer2.getChord().getFingerRaw(0).key.toLong());
        assertEquals(  0, peer2.getChord().getFingerRaw(1).key.toLong());
        assertEquals(  0, peer2.getChord().getFingerRaw(2).key.toLong());
        assertEquals(  0, peer2.getChord().getFingerRaw(3).key.toLong());
        assertEquals(  0, peer2.getChord().getFingerRaw(4).key.toLong());
        assertEquals(  0, peer2.getChord().getFingerRaw(5).key.toLong());
        assertEquals(  0, peer2.getChord().getFingerRaw(6).key.toLong());
        assertEquals(  0, peer2.getChord().getFingerRaw(7).key.toLong());
        assertEquals(800, peer2.getChord().getFingerRaw(8).key.toLong());
        assertEquals(800, peer2.getChord().getFingerRaw(9).key.toLong());

        assertTrue(peer1.die());

        assertEquals(  0, peer2.getChord().getFingerRaw(0).key.toLong());
        assertEquals(  0, peer2.getChord().getFingerRaw(1).key.toLong());
        assertEquals(  0, peer2.getChord().getFingerRaw(2).key.toLong());
        assertEquals(  0, peer2.getChord().getFingerRaw(3).key.toLong());
        assertEquals(  0, peer2.getChord().getFingerRaw(4).key.toLong());
        assertEquals(  0, peer2.getChord().getFingerRaw(5).key.toLong());
        assertEquals(  0, peer2.getChord().getFingerRaw(6).key.toLong());
        assertEquals(  0, peer2.getChord().getFingerRaw(7).key.toLong());
        assertEquals(800, peer2.getChord().getFingerRaw(8).key.toLong());
        assertEquals(800, peer2.getChord().getFingerRaw(9).key.toLong());

        assertEquals(800, peer2.getChord().getFingerInfo(0).key.toLong());
        assertEquals(800, peer2.getChord().getFingerInfo(1).key.toLong());
        assertEquals(800, peer2.getChord().getFingerInfo(2).key.toLong());
        assertEquals(800, peer2.getChord().getFingerInfo(3).key.toLong());
        assertEquals(800, peer2.getChord().getFingerInfo(4).key.toLong());
        assertEquals(800, peer2.getChord().getFingerInfo(5).key.toLong());
        assertEquals(800, peer2.getChord().getFingerInfo(6).key.toLong());
        assertEquals(800, peer2.getChord().getFingerInfo(7).key.toLong());
        assertEquals(800, peer2.getChord().getFingerInfo(8).key.toLong());
        assertEquals(800, peer2.getChord().getFingerInfo(9).key.toLong());

        assertEquals(800, peer2.getChord().getFingerRaw(0).key.toLong());
        assertEquals(800, peer2.getChord().getFingerRaw(1).key.toLong());
        assertEquals(800, peer2.getChord().getFingerRaw(2).key.toLong());
        assertEquals(800, peer2.getChord().getFingerRaw(3).key.toLong());
        assertEquals(800, peer2.getChord().getFingerRaw(4).key.toLong());
        assertEquals(800, peer2.getChord().getFingerRaw(5).key.toLong());
        assertEquals(800, peer2.getChord().getFingerRaw(6).key.toLong());
        assertEquals(800, peer2.getChord().getFingerRaw(7).key.toLong());
        assertEquals(800, peer2.getChord().getFingerRaw(8).key.toLong());
        assertEquals(800, peer2.getChord().getFingerRaw(9).key.toLong());

        assertTrue(peer2.leave());
    }

    @Test(timeout=1000)
    public void die_2peers_2() throws Exception {
        int KEY_SIZE = 10;

        Peer peer1 = new Peer(KEY_SIZE, 0, InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peer1.join());

        Peer peer2 = new Peer(KEY_SIZE, 800, InetAddress.getByName("localhost"), Paths.get("bin"));
        peer2.join(peer1.getSocketAddress());

        assertTrue(peer1.die());

        assertEquals(  0, peer2.getChord().getFingerRaw(0).key.toLong());
        assertEquals(  0, peer2.getChord().getFingerRaw(1).key.toLong());
        assertEquals(  0, peer2.getChord().getFingerRaw(2).key.toLong());
        assertEquals(  0, peer2.getChord().getFingerRaw(3).key.toLong());
        assertEquals(  0, peer2.getChord().getFingerRaw(4).key.toLong());
        assertEquals(  0, peer2.getChord().getFingerRaw(5).key.toLong());
        assertEquals(  0, peer2.getChord().getFingerRaw(6).key.toLong());
        assertEquals(  0, peer2.getChord().getFingerRaw(7).key.toLong());
        assertEquals(800, peer2.getChord().getFingerRaw(8).key.toLong());
        assertEquals(800, peer2.getChord().getFingerRaw(9).key.toLong());

        assertTrue(peer2.fix());

        assertEquals(800, peer2.getChord().getFingerRaw(0).key.toLong());
        assertEquals(800, peer2.getChord().getFingerRaw(1).key.toLong());
        assertEquals(800, peer2.getChord().getFingerRaw(2).key.toLong());
        assertEquals(800, peer2.getChord().getFingerRaw(3).key.toLong());
        assertEquals(800, peer2.getChord().getFingerRaw(4).key.toLong());
        assertEquals(800, peer2.getChord().getFingerRaw(5).key.toLong());
        assertEquals(800, peer2.getChord().getFingerRaw(6).key.toLong());
        assertEquals(800, peer2.getChord().getFingerRaw(7).key.toLong());
        assertEquals(800, peer2.getChord().getFingerRaw(8).key.toLong());
        assertEquals(800, peer2.getChord().getFingerRaw(9).key.toLong());

        assertTrue(peer2.leave());
    }

    @Test(timeout=1000)
    public void die_2peers_withFiles() throws Exception {
        int KEY_SIZE = 10;

        Peer peer1 = new Peer(KEY_SIZE, 0, InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peer1.join());

        Peer peer2 = new Peer(KEY_SIZE, 800, InetAddress.getByName("localhost"), Paths.get("bin"));
        peer2.join(peer1.getSocketAddress());

        Username username = new Username("user1");
        Password password = new Password("1234");
        Main.Path path = new Main.Path("mydata");
        byte[] data = "my data".getBytes();
        ChunkIterator chunkIterator = new ByteArrayChunkIterator(data, Main.CHUNK_SIZE);

        assertTrue(peer1.backup(username, password, path, 2, chunkIterator));
        assertArrayEquals(data, peer1.getDataStorage().getLocalDataStorage().get(new UUID("user1/mydata-0-0")));
        assertArrayEquals(data, peer2.getDataStorage().getLocalDataStorage().get(new UUID("user1/mydata-0-1")));

        assertTrue(peer2.die());
        assertArrayEquals(data, peer1.getDataStorage().getLocalDataStorage().get(new UUID("user1/mydata-0-0")));
        assertNull(peer1.getDataStorage().getLocalDataStorage().get(new UUID("user1/mydata-0-1")));

        assertTrue(peer1.fix());
        assertArrayEquals(data, peer1.getDataStorage().getLocalDataStorage().get(new UUID("user1/mydata-0-0")));
        assertArrayEquals(data, peer1.getDataStorage().getLocalDataStorage().get(new UUID("user1/mydata-0-1")));

        assertTrue(peer1.leave());
    }
}
