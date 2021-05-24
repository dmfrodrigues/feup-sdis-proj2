package sdis.Modules.Main;

import org.junit.Test;
import sdis.Peer;
import sdis.Storage.ByteArrayChunkIterator;
import sdis.Storage.ChunkIterator;
import sdis.Storage.ChunkOutput;
import sdis.Storage.DataBuilderChunkOutput;
import sdis.UUID;
import sdis.Utils.DataBuilder;

import java.net.InetAddress;
import java.nio.file.Paths;
import java.util.HashSet;

import static org.junit.Assert.*;

public class MainTest {

    @Test(timeout=10000)
    public void backupFileProtocol_noEnlist_1peer() throws Exception {
        int KEY_SIZE = 10;

        Peer peer = new Peer(KEY_SIZE, 0, InetAddress.getByName("localhost"), Paths.get("bin"));
        peer.join().get();
        Main main = peer.getMain();

        byte[] data = "my data".getBytes();
        Main.File file = new Username("user1").asFile(1);

        BackupFileProtocol backupFileProtocol = new BackupFileProtocol(main, file, data, 10, false);
        assertTrue(backupFileProtocol.get());

        DataBuilder dataBuilder = new DataBuilder();
        ChunkOutput chunkOutput = new DataBuilderChunkOutput(dataBuilder, 10);
        RestoreFileProtocol restoreFileProtocol = new RestoreFileProtocol(main, file, chunkOutput, 10);
        assertTrue(restoreFileProtocol.get());

        assertArrayEquals(data, dataBuilder.get());

        peer.leave().get();
    }

    @Test(timeout=10000)
    public void backupFileProtocol_noEnlist_10peer() throws Exception {
        int KEY_SIZE = 10;

        int[] ids = { 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000 };

        Peer[] peers = new Peer[ids.length];
        for(int i = 0; i < ids.length; ++i)
            peers[i] = new Peer(KEY_SIZE, ids[i], InetAddress.getByName("localhost"), Paths.get("bin"));
        peers[0].join().get();
        for(int i = 1; i < ids.length; ++i)
            peers[i].join(peers[0].getSocketAddress()).join();

        byte[] data = "my data".getBytes();
        Main.File file = new Username("user1").asFile(1);

        BackupFileProtocol backupFileProtocol = new BackupFileProtocol(peers[0].getMain(), file, data, 10, false);
        assertTrue(backupFileProtocol.get());

        DataBuilder dataBuilder = new DataBuilder();
        ChunkOutput chunkOutput = new DataBuilderChunkOutput(dataBuilder, 10);
        RestoreFileProtocol restoreFileProtocol = new RestoreFileProtocol(peers[0].getMain(), file, chunkOutput, 10);
        assertTrue(restoreFileProtocol.get());

        assertArrayEquals(data, dataBuilder.get());

        for(Peer p: peers) p.leave().get();
    }

    @Test(timeout=1000)
    public void userMetadata_serialize() throws Exception {
        Username username = new Username("user1");
        Password password = new Password("1234");

        UserMetadata userMetadata = new UserMetadata(username, password);

        byte[] data = userMetadata.serialize();
        assertNotNull(data);
        assertEquals(439, data.length);
    }

    @Test(timeout=1000)
    public void authenticate_1peer() throws Exception {
        int KEY_SIZE = 10;

        Peer peer = new Peer(KEY_SIZE, 0, InetAddress.getByName("localhost"), Paths.get("bin"));
        peer.join().get();

        Username username = new Username("user1");
        Password password = new Password("1234");

        UserMetadata userMetadata = peer.authenticate(username, password);

        assertNotNull(userMetadata);
        assertEquals(username, userMetadata.getUsername());
        assertEquals(password, userMetadata.getPassword());
        assertEquals(new HashSet<Main.Path>(), userMetadata.getFiles());

        peer.leave().get();
    }

    @Test(timeout=1000)
    public void authenticate_10peers() throws Exception {
        int KEY_SIZE = 10;

        int[] ids = { 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000 };

        Peer[] peers = new Peer[ids.length];
        for(int i = 0; i < ids.length; ++i)
            peers[i] = new Peer(KEY_SIZE, ids[i], InetAddress.getByName("localhost"), Paths.get("bin"));
        peers[0].join().get();
        for(int i = 1; i < ids.length; ++i)
            peers[i].join(peers[0].getSocketAddress()).join();

        for(int i = 0; i < ids.length; ++i) {
            Username username = new Username("user" + i);
            Password password = new Password("1234");
            UserMetadata userMetadata = peers[i].authenticate(username, password);

            assertNotNull(userMetadata);
            assertEquals(username, userMetadata.getUsername());
            assertEquals(password, userMetadata.getPassword());
            assertEquals(new HashSet<Main.Path>(), userMetadata.getFiles());
        }
        for(int i = 0; i < ids.length; ++i){
            int j = (i+ids.length/2)%ids.length;

            Username username = new Username("user" + j);
            Password password = new Password("1234");
            UserMetadata userMetadata = peers[i].authenticate(username, password);

            assertNotNull(userMetadata);
            assertEquals(username, userMetadata.getUsername());
            assertEquals(password, userMetadata.getPassword());
            assertEquals(new HashSet<Main.Path>(), userMetadata.getFiles());
        }

        for(Peer p: peers) p.leave().get();
    }

    @Test(timeout=10000)
    public void backupFileProtocol_simple_1peer() throws Exception {
        int KEY_SIZE = 10;

        Username username = new Username("user1");
        Password password = new Password("1234");
        byte[] data = "my data".getBytes();
        Main.Path path = new Main.Path("mydata");
        Main.File file = new Main.File(username, path, 1, 1);

        Peer peer = new Peer(KEY_SIZE, 0, InetAddress.getByName("localhost"), Paths.get("bin"));
        peer.join().get();

        peer.authenticate(username, password);

        assertTrue(peer.getMain().getSystemStorage().getDataStorage().has(new UUID("user1-0-0")));

        BackupFileProtocol backupFileProtocol = new BackupFileProtocol(peer.getMain(), file, data, 10, false);
        assertTrue(backupFileProtocol.enlistFile().get());

        UserMetadata userMetadata = peer.authenticate(username, password);
        assertNotNull(userMetadata.getFile(path));

        peer.leave().get();
    }

    @Test(timeout=10000)
    public void backupFileProtocol_1peer() throws Exception {
        int KEY_SIZE = 10;

        Username username = new Username("user1");
        Password password = new Password("1234");
        byte[] data = "my data".getBytes();
        Main.Path path = new Main.Path("mydata");
        ChunkIterator chunkIterator = new ByteArrayChunkIterator(data, Main.CHUNK_SIZE);

        Peer peer = new Peer(KEY_SIZE, 0, InetAddress.getByName("localhost"), Paths.get("bin"));
        peer.join().get();

        assertTrue(peer.backup(username, password, path, 1, chunkIterator));

        UserMetadata userMetadata = peer.authenticate(username, password);
        assertNotNull(userMetadata.getFile(path));

        DataBuilder builder = new DataBuilder();
        ChunkOutput chunkOutput = new DataBuilderChunkOutput(builder, 10);
        assertTrue(peer.restore(username, password, path, chunkOutput));

        assertArrayEquals(data, builder.get());

        peer.leave().get();
    }

    @Test(timeout=10000)
    public void backupFileProtocol_10peer() throws Exception {
        int KEY_SIZE = 10;

        int[] ids = {100, 200, 300, 400, 500, 600, 700, 800, 900, 1000};

        Peer[] peers = new Peer[ids.length];
        for (int i = 0; i < ids.length; ++i)
            peers[i] = new Peer(KEY_SIZE, ids[i], InetAddress.getByName("localhost"), Paths.get("bin"));
        peers[0].join().get();
        for (int i = 1; i < ids.length; ++i)
            peers[i].join(peers[0].getSocketAddress()).join();

        byte[] data = "my data".getBytes();
        Main.File file = new Username("user1").asFile(1);

        BackupFileProtocol backupFileProtocol = new BackupFileProtocol(peers[0].getMain(), file, data, 10, false);
        assertTrue(backupFileProtocol.get());

        DataBuilder dataBuilder = new DataBuilder();
        ChunkOutput chunkOutput = new DataBuilderChunkOutput(dataBuilder, 10);
        RestoreFileProtocol restoreFileProtocol = new RestoreFileProtocol(peers[0].getMain(), file, chunkOutput, 10);
        assertTrue(restoreFileProtocol.get());

        assertArrayEquals(data, dataBuilder.get());

        for (Peer p : peers) p.leave().get();
    }

    @Test(timeout=10000)
    public void backupMain_1peer() throws Exception {
        int KEY_SIZE = 10;

        Peer peer = new Peer(KEY_SIZE, 0, InetAddress.getByName("localhost"), Paths.get("bin"));

        peer.join().get();

        Username username = new Username("user");
        Password password = new Password("1234");

        Main.Path path = new Main.Path("data");
        byte[] data = ("my data").getBytes();
        ChunkIterator chunkIterator = new ByteArrayChunkIterator(data, Main.CHUNK_SIZE);

        UserMetadata userMetadata = peer.authenticate(username, password);
        assertNotNull(userMetadata);
        Main.File file = new Main.File(username, path, chunkIterator.length(), 1);
        assertTrue(peer.getMain().backupFile(file, chunkIterator).get());

        userMetadata = peer.authenticate(username, password);
        assertNotNull(userMetadata);
        assertNotNull(userMetadata.getFile(path));

        peer.leave().get();
    }

    @Test(timeout=10000)
    public void backup_1peer() throws Exception {
        int KEY_SIZE = 10;

        Peer peer = new Peer(KEY_SIZE, 0, InetAddress.getByName("localhost"), Paths.get("bin"));

        peer.join().get();

        Username username = new Username("user");
        Password password = new Password("1234");

        Main.Path path = new Main.Path("data");
        byte[] data = ("my data").getBytes();

        assertTrue(peer.backup(username, password, path, 1, new ByteArrayChunkIterator(data, Main.CHUNK_SIZE)));

        UserMetadata userMetadata = peer.authenticate(username, password);
        assertNotNull(userMetadata);
        assertNotNull(userMetadata.getFile(path));

        peer.leave().get();
    }

    @Test(timeout=10000)
    public void backup_2peers() throws Exception {
        int KEY_SIZE = 10;

        Peer peer1 = new Peer(KEY_SIZE, 0, InetAddress.getByName("localhost"), Paths.get("bin"));
        Peer peer2 = new Peer(KEY_SIZE, 500, InetAddress.getByName("localhost"), Paths.get("bin"));

        peer1.join().get();
        peer2.join(peer1.getSocketAddress()).get();

        Username username = new Username("user");
        Password password = new Password("1234");

        Main.Path path = new Main.Path("data");
        byte[] data = ("my data").getBytes();

        assertTrue(peer1.backup(username, password, path, 1, new ByteArrayChunkIterator(data, Main.CHUNK_SIZE)));

        UserMetadata userMetadata = peer1.authenticate(username, password);
        assertNotNull(userMetadata);
        assertNotNull(userMetadata.getFile(path));

        userMetadata = peer2.authenticate(username, password);
        assertNotNull(userMetadata);
        assertNotNull(userMetadata.getFile(path));

        peer1.leave().get();
        peer2.leave().get();
    }

    @Test(timeout=10000)
    public void restore_2peers() throws Exception {
        int KEY_SIZE = 10;

        Peer peer1 = new Peer(KEY_SIZE, 0, InetAddress.getByName("localhost"), Paths.get("bin"));
        Peer peer2 = new Peer(KEY_SIZE, 500, InetAddress.getByName("localhost"), Paths.get("bin"));

        peer1.join().get();
        peer2.join(peer1.getSocketAddress()).get();

        Username username = new Username("user");
        Password password = new Password("1234");

        Main.Path path = new Main.Path("data");
        byte[] data = ("my data").getBytes();

        assertTrue(peer1.backup(username, password, path, 1, new ByteArrayChunkIterator(data, Main.CHUNK_SIZE)));

        DataBuilder builder = new DataBuilder();
        DataBuilderChunkOutput chunkOutput = new DataBuilderChunkOutput(builder, 10);

        assertTrue(peer2.restore(username, password, path, chunkOutput));
        assertArrayEquals(data, builder.get());

        peer1.leave().get();
        peer2.leave().get();
    }

    @Test(timeout=10000)
    public void backup_manyFiles_10peer() throws Exception {
        int KEY_SIZE = 10;

        int[] ids = { 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000 };

        Peer[] peers = new Peer[ids.length];
        for(int i = 0; i < ids.length; ++i)
            peers[i] = new Peer(KEY_SIZE, ids[i], InetAddress.getByName("localhost"), Paths.get("bin"));
        peers[0].join().get();
        for(int i = 1; i < ids.length; ++i)
            peers[i].join(peers[0].getSocketAddress()).join();

        for(int i = 0; i < ids.length; ++i){
            Username username = new Username("user" + i);
            Password password = new Password(Integer.toString(i));

            Main.Path path = new Main.Path("data");
            byte[] data = ("data" + i).getBytes();

            assertTrue(peers[i].backup(username, password, path, 1, new ByteArrayChunkIterator(data, Main.CHUNK_SIZE)));
        }

        for(int i = 0; i < ids.length; ++i){
            int j = (i+ids.length/2)%ids.length;

            Username username = new Username("user" + j);
            Password password = new Password(Integer.toString(j));

            Main.Path path = new Main.Path("data");
            byte[] data = ("data" + j).getBytes();

            DataBuilder builder = new DataBuilder();
            DataBuilderChunkOutput chunkOutput = new DataBuilderChunkOutput(builder, 10);

            assertTrue(peers[i].restore(username, password, path, chunkOutput));
            assertArrayEquals(data, builder.get());
        }

        for(Peer p: peers) p.leave().get();
    }

    @Test(timeout=10000)
    public void delete_manyFiles_10peer() throws Exception {
        int KEY_SIZE = 10;

        int[] ids = { 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000 };

        Peer[] peers = new Peer[ids.length];
        for(int i = 0; i < ids.length; ++i)
            peers[i] = new Peer(KEY_SIZE, ids[i], InetAddress.getByName("localhost"), Paths.get("bin"));
        peers[0].join().get();
        for(int i = 1; i < ids.length; ++i)
            peers[i].join(peers[0].getSocketAddress()).join();

        for(int i = 0; i < ids.length; ++i){
            Username username = new Username("user" + i);
            Password password = new Password(Integer.toString(i));

            Main.Path path = new Main.Path("data");
            byte[] data = ("data" + i).getBytes();

            assertTrue(peers[i].backup(username, password, path, 1, new ByteArrayChunkIterator(data, Main.CHUNK_SIZE)));
        }

        int[] toDelete = {8, 3, 0, 9, 6, 1, 5, 7, 2, 4};
        for(int i = 0; i < ids.length; ++i){
            System.out.println("i=" + i);

            int j = toDelete[i];

            Username username = new Username("user" + j);
            Password password = new Password(Integer.toString(j));

            Main.Path path = new Main.Path("data");
            DataBuilder builder = new DataBuilder();
            ChunkOutput chunkOutput = new DataBuilderChunkOutput(builder, 10);

            assertTrue(peers[i].delete(username, password, path));
            assertFalse(peers[i].restore(username, password, path, chunkOutput));
            assertEquals(0, builder.get().length);
        }

        for(Peer p: peers) p.leave().get();
    }

}
