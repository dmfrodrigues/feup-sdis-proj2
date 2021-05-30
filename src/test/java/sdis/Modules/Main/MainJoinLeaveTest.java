package sdis.Modules.Main;

import org.junit.Ignore;
import org.junit.Test;
import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.DataStorage;
import sdis.Modules.ProtocolTask;
import sdis.Modules.SystemStorage.RemoveKeysProtocol;
import sdis.Peer;
import sdis.Storage.ByteArrayChunkIterator;
import sdis.Storage.ChunkIterator;
import sdis.UUID;
import sdis.Utils.Utils;

import java.net.InetAddress;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class MainJoinLeaveTest {

    @Test(timeout=10000)
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
    public void leave_2peers_primitives() throws Exception {
        int KEY_SIZE = 10;

        Peer peer1 = new Peer(KEY_SIZE, 0, InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peer1.join());
        DataStorage dataStorage1 = peer1.getDataStorage();

        Peer peer2 = new Peer(KEY_SIZE, 500, InetAddress.getByName("localhost"), Paths.get("bin"));
        peer2.join(peer1.getSocketAddress());
        DataStorage dataStorage2 = peer2.getDataStorage();
        Chord chord2 = peer2.getChord();

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

        assertTrue(peer1.getChord().leave(new ProtocolTask<>() {
            @Override
            protected Boolean compute() {
                return true;
            }
        }));
        for(int i = 0; i < chord2.getMod(); ++i)
            assertEquals(chord2.getNodeInfo(), chord2.findSuccessor(chord2.newKey(i)));

        // Remove keys
        RemoveKeysProtocol removeKeysProtocol = new RemoveKeysProtocol(peer1.getSystemStorage());
        assertTrue(removeKeysProtocol.invoke());

        // Delete local storage
        assertTrue(Utils.deleteRecursive(dataStorage1.getLocalDataStorage().getStoragePath().toFile()));

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

    @Ignore
    @Test(timeout=10000)
    public void die_2peers() throws Exception {
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

        assertTrue(peer1.die());

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
}
