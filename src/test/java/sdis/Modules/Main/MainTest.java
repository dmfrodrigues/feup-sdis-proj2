package sdis.Modules.Main;

import org.junit.Test;
import sdis.Modules.DataStorage.DataStorage;
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

    @Test(timeout=1000)
    public void backupFileProtocol_noEnlist_1peer() throws Exception {
        int KEY_SIZE = 10;

        Peer peer = new Peer(KEY_SIZE, 0, InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peer.join());
        Main main = peer.getMain();

        byte[] data = "my data".getBytes();
        Main.File file = new Username("user1").asFile(1);

        BackupFileProtocol backupFileProtocol = new BackupFileProtocol(main, file, data, false);
        assertTrue(backupFileProtocol.invoke());

        DataBuilder dataBuilder = new DataBuilder();
        ChunkOutput chunkOutput = new DataBuilderChunkOutput(dataBuilder, 10);
        RestoreFileProtocol restoreFileProtocol = new RestoreFileProtocol(main, file, chunkOutput);
        assertTrue(restoreFileProtocol.invoke());

        assertArrayEquals(data, dataBuilder.get());

        peer.leave();
    }

    @Test(timeout=1000)
    public void backupFileProtocol_noEnlist_10peer() throws Exception {
        int KEY_SIZE = 10;

        int[] ids = { 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000 };

        Peer[] peers = new Peer[ids.length];
        for(int i = 0; i < ids.length; ++i)
            peers[i] = new Peer(KEY_SIZE, ids[i], InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peers[0].join());
        for(int i = 1; i < ids.length; ++i)
            peers[i].join(peers[0].getSocketAddress());

        byte[] data = "my data".getBytes();
        Main.File file = new Username("user1").asFile(1);

        BackupFileProtocol backupFileProtocol = new BackupFileProtocol(peers[0].getMain(), file, data, false);
        assertTrue(backupFileProtocol.invoke());

        DataBuilder dataBuilder = new DataBuilder();
        ChunkOutput chunkOutput = new DataBuilderChunkOutput(dataBuilder, 10);
        RestoreFileProtocol restoreFileProtocol = new RestoreFileProtocol(peers[0].getMain(), file, chunkOutput);
        assertTrue(restoreFileProtocol.invoke());

        assertArrayEquals(data, dataBuilder.get());

        for(Peer p: peers) p.leave();
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
        assertTrue(peer.join());

        Username username = new Username("user1");
        Password password = new Password("1234");

        UserMetadata userMetadata = peer.authenticate(username, password);

        assertNotNull(userMetadata);
        assertEquals(username, userMetadata.getUsername());
        assertEquals(password, userMetadata.getPassword());
        assertEquals(new HashSet<Main.Path>(), userMetadata.getFiles());

        assertNull(peer.authenticate(username, new Password("12345")));
        assertNotNull(peer.authenticate(username, password));

        peer.leave();
    }

    @Test(timeout=2000)
    public void authenticate_10peers() throws Exception {
        int KEY_SIZE = 10;

        int[] ids = { 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000 };

        Peer[] peers = new Peer[ids.length];
        for(int i = 0; i < ids.length; ++i)
            peers[i] = new Peer(KEY_SIZE, ids[i], InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peers[0].join());
        for(int i = 1; i < ids.length; ++i)
            peers[i].join(peers[0].getSocketAddress());

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

        for(Peer p: peers) p.leave();
    }

    @Test(timeout=1000)
    public void backupFileProtocol_simple_1peer() throws Exception {
        int KEY_SIZE = 10;

        Username username = new Username("user1");
        Password password = new Password("1234");
        byte[] data = "my data".getBytes();
        Main.Path path = new Main.Path("mydata");
        Main.File file = new Main.File(username, path, 1, 1);

        Peer peer = new Peer(KEY_SIZE, 0, InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peer.join());

        peer.authenticate(username, password);

        assertTrue(peer.getMain().getSystemStorage().getDataStorage().has(new UUID("user1-0-0")));

        BackupFileProtocol backupFileProtocol = new BackupFileProtocol(peer.getMain(), file, data, false);
        assertTrue(backupFileProtocol.enlistFile());

        UserMetadata userMetadata = peer.authenticate(username, password);
        assertNotNull(userMetadata.getFile(path));

        peer.leave();
    }

    @Test(timeout=1000)
    public void backupFileProtocol_1peer() throws Exception {
        int KEY_SIZE = 10;

        Username username = new Username("user1");
        Password password = new Password("1234");
        byte[] data = "my data".getBytes();
        Main.Path path = new Main.Path("mydata");
        ChunkIterator chunkIterator = new ByteArrayChunkIterator(data, Main.CHUNK_SIZE);

        Peer peer = new Peer(KEY_SIZE, 0, InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peer.join());

        assertTrue(peer.backup(username, password, path, 1, chunkIterator));

        UserMetadata userMetadata = peer.authenticate(username, password);
        assertNotNull(userMetadata.getFile(path));

        DataBuilder builder = new DataBuilder();
        ChunkOutput chunkOutput = new DataBuilderChunkOutput(builder, 10);
        assertTrue(peer.restore(username, password, path, chunkOutput));

        assertArrayEquals(data, builder.get());

        peer.leave();
    }

    @Test(timeout=1000)
    public void backupFileProtocol_10peer() throws Exception {
        int KEY_SIZE = 10;

        int[] ids = {100, 200, 300, 400, 500, 600, 700, 800, 900, 1000};

        Peer[] peers = new Peer[ids.length];
        for (int i = 0; i < ids.length; ++i)
            peers[i] = new Peer(KEY_SIZE, ids[i], InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peers[0].join());
        for (int i = 1; i < ids.length; ++i)
            peers[i].join(peers[0].getSocketAddress());

        byte[] data = "my data".getBytes();
        Main.File file = new Username("user1").asFile(1);

        BackupFileProtocol backupFileProtocol = new BackupFileProtocol(peers[0].getMain(), file, data, false);
        assertTrue(backupFileProtocol.invoke());

        DataBuilder dataBuilder = new DataBuilder();
        ChunkOutput chunkOutput = new DataBuilderChunkOutput(dataBuilder, 10);
        RestoreFileProtocol restoreFileProtocol = new RestoreFileProtocol(peers[0].getMain(), file, chunkOutput);
        assertTrue(restoreFileProtocol.invoke());

        assertArrayEquals(data, dataBuilder.get());

        for (Peer p : peers) p.leave();
    }

    @Test(timeout=1000)
    public void backupMain_1peer() throws Exception {
        int KEY_SIZE = 10;

        Peer peer = new Peer(KEY_SIZE, 0, InetAddress.getByName("localhost"), Paths.get("bin"));

        assertTrue(peer.join());

        Username username = new Username("user");
        Password password = new Password("1234");

        Main.Path path = new Main.Path("data");
        byte[] data = ("my data").getBytes();
        ChunkIterator chunkIterator = new ByteArrayChunkIterator(data, Main.CHUNK_SIZE);

        UserMetadata userMetadata = peer.authenticate(username, password);
        assertNotNull(userMetadata);
        Main.File file = new Main.File(username, path, chunkIterator.length(), 1);
        assertTrue(peer.getMain().backupFile(file, chunkIterator));

        userMetadata = peer.authenticate(username, password);
        assertNotNull(userMetadata);
        assertNotNull(userMetadata.getFile(path));

        peer.leave();
    }

    @Test(timeout=1000)
    public void backup_1peer() throws Exception {
        int KEY_SIZE = 10;

        Peer peer = new Peer(KEY_SIZE, 0, InetAddress.getByName("localhost"), Paths.get("bin"));

        assertTrue(peer.join());

        Username username = new Username("user");
        Password password = new Password("1234");

        Main.Path path = new Main.Path("data");
        byte[] data = ("my data").getBytes();

        assertTrue(peer.backup(username, password, path, 1, new ByteArrayChunkIterator(data, Main.CHUNK_SIZE)));

        UserMetadata userMetadata = peer.authenticate(username, password);
        assertNotNull(userMetadata);
        assertNotNull(userMetadata.getFile(path));

        peer.leave();
    }

    @Test(timeout=1000)
    public void backup_2peers() throws Exception {
        int KEY_SIZE = 10;

        Peer peer1 = new Peer(KEY_SIZE, 0, InetAddress.getByName("localhost"), Paths.get("bin"));
        Peer peer2 = new Peer(KEY_SIZE, 500, InetAddress.getByName("localhost"), Paths.get("bin"));

        assertTrue(peer1.join());
        peer2.join(peer1.getSocketAddress());

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

        peer1.leave();
        peer2.leave();
    }

    @Test(timeout=1000)
    public void backup_1peers_large_1() throws Exception {
        int KEY_SIZE = 10;

        Peer peer1 = new Peer(KEY_SIZE, 0, InetAddress.getByName("localhost"), Paths.get("bin"));

        assertTrue(peer1.join());

        Username username = new Username("user");
        Password password = new Password("1234");

        Main.Path path1 = new Main.Path("data1");
        byte[] data1 = new byte[64000];
        for(int i = 0; i < data1.length; ++i)
            data1[i] = Integer.toString(i % 10).getBytes()[0];

        assertTrue(peer1.backup(username, password, path1, 1, new ByteArrayChunkIterator(data1, Main.CHUNK_SIZE)));

        assertArrayEquals(data1, peer1.getSystemStorage().getDataStorage().getLocalDataStorage().get(new UUID("user/data1-0-0")));
        assertArrayEquals(new byte[0], peer1.getSystemStorage().getDataStorage().getLocalDataStorage().get(new UUID("user/data1-1-0")));

        UserMetadata userMetadata = peer1.authenticate(username, password);
        assertNotNull(userMetadata);
        assertNotNull(userMetadata.getFile(path1));
        DataBuilder builder = new DataBuilder();
        DataBuilderChunkOutput chunkOutput = new DataBuilderChunkOutput(builder, 10);
        assertTrue(peer1.restore(username, password, path1, chunkOutput));
        assertArrayEquals(data1, builder.get());

        peer1.leave();
    }

    @Test(timeout=1000)
    public void backup_1peers_large_2() throws Exception {
        int KEY_SIZE = 10;

        Peer peer1 = new Peer(KEY_SIZE, 0, InetAddress.getByName("localhost"), Paths.get("bin"));

        assertTrue(peer1.join());

        Username username = new Username("user");
        Password password = new Password("1234");

        Main.Path path1 = new Main.Path("data1");
        byte[] data1 = new byte[64001];
        for(int i = 0; i < data1.length; ++i)
            data1[i] = Integer.toString(i % 9).getBytes()[0];

        assertTrue(peer1.backup(username, password, path1, 1, new ByteArrayChunkIterator(data1, Main.CHUNK_SIZE)));

        UserMetadata userMetadata = peer1.authenticate(username, password);
        assertNotNull(userMetadata);
        assertNotNull(userMetadata.getFile(path1));
        DataBuilder builder = new DataBuilder();
        DataBuilderChunkOutput chunkOutput = new DataBuilderChunkOutput(builder, 10);
        assertTrue(peer1.restore(username, password, path1, chunkOutput));
        assertArrayEquals(data1, builder.get());

        peer1.leave();
    }


    @Test(timeout=1000)
    public void backup_1peers_large_3() throws Exception {
        int KEY_SIZE = 10;

        Peer peer1 = new Peer(KEY_SIZE, 0, InetAddress.getByName("localhost"), Paths.get("bin"));

        assertTrue(peer1.join());

        Username username = new Username("user");
        Password password = new Password("1234");

        Main.Path path2 = new Main.Path("data2");
        byte[] data2 = new byte[128000];
        for(int i = 0; i < data2.length; ++i)
            data2[i] = Integer.toString(i % 9).getBytes()[0];

        assertTrue(peer1.backup(username, password, path2, 1, new ByteArrayChunkIterator(data2, Main.CHUNK_SIZE)));
        DataBuilder builder = new DataBuilder();
        DataBuilderChunkOutput chunkOutput = new DataBuilderChunkOutput(builder, 10);
        assertTrue(peer1.restore(username, password, path2, chunkOutput));
        assertArrayEquals(data2, builder.get());

        peer1.leave();
    }

    @Test(timeout=1000)
    public void restore_2peers() throws Exception {
        int KEY_SIZE = 10;

        Peer peer1 = new Peer(KEY_SIZE, 0, InetAddress.getByName("localhost"), Paths.get("bin"));
        Peer peer2 = new Peer(KEY_SIZE, 500, InetAddress.getByName("localhost"), Paths.get("bin"));

        assertTrue(peer1.join());
        peer2.join(peer1.getSocketAddress());

        Username username = new Username("user");
        Password password = new Password("1234");

        Main.Path path = new Main.Path("data");
        byte[] data = ("my data").getBytes();

        assertTrue(peer1.backup(username, password, path, 1, new ByteArrayChunkIterator(data, Main.CHUNK_SIZE)));

        DataBuilder builder = new DataBuilder();
        DataBuilderChunkOutput chunkOutput = new DataBuilderChunkOutput(builder, 10);

        assertTrue(peer2.restore(username, password, path, chunkOutput));
        assertArrayEquals(data, builder.get());

        peer1.leave();
        peer2.leave();
    }

    @Test(timeout=3000)
    public void backup_manyFiles_10peer() throws Exception {
        int KEY_SIZE = 10;

        int[] ids = { 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000 };

        Peer[] peers = new Peer[ids.length];
        for(int i = 0; i < ids.length; ++i)
            peers[i] = new Peer(KEY_SIZE, ids[i], InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peers[0].join());
        for(int i = 1; i < ids.length; ++i)
            peers[i].join(peers[0].getSocketAddress());

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

        for(Peer p: peers) p.leave();
    }

    @Test(timeout=3000)
    public void delete_manyFiles_10peer() throws Exception {
        int KEY_SIZE = 10;

        int[] ids = { 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000 };

        Peer[] peers = new Peer[ids.length];
        for(int i = 0; i < ids.length; ++i)
            peers[i] = new Peer(KEY_SIZE, ids[i], InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peers[0].join());
        for(int i = 1; i < ids.length; ++i)
            peers[i].join(peers[0].getSocketAddress());

        for(int i = 0; i < ids.length; ++i){
            Username username = new Username("user" + i);
            Password password = new Password(Integer.toString(i));

            Main.Path path = new Main.Path("data");
            byte[] data = ("data" + i).getBytes();

            assertTrue(peers[i].backup(username, password, path, 1, new ByteArrayChunkIterator(data, Main.CHUNK_SIZE)));
        }

        int[] toDelete = {8, 3, 0, 9, 6, 1, 5, 7, 2, 4};
        for(int i = 0; i < ids.length; ++i){
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

        for(Peer p: peers) p.leave();
    }

    @Test(timeout=1000)
    public void deleteAccount_1peer_noBackup() throws Exception {
        int KEY_SIZE = 10;

        Peer peer = new Peer(KEY_SIZE, 0, InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peer.join());

        Username username = new Username("user");
        Password password = new Password("1234");

        assertNotNull(peer.authenticate(username, password));
        assertTrue(peer.deleteAccount(username, password));

        assertTrue(peer.leave());
    }

    @Test(timeout=1000)
    public void deleteAccount_1peer() throws Exception {
        int KEY_SIZE = 10;

        Peer peer = new Peer(KEY_SIZE, 0, InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peer.join());
        DataStorage dataStorage = peer.getDataStorage();

        Username username = new Username("user");
        Password password = new Password("1234");

        Main.Path path = new Main.Path("data");
        Main.File file = new Main.File(username, path, 1, 1);
        UUID id = file.getChunk(0).getReplica(0).getUUID();
        byte[] data = ("my data").getBytes();

        assertTrue(peer.backup(username, password, path, 1, new ByteArrayChunkIterator(data, Main.CHUNK_SIZE)));
        assertNotNull(dataStorage.get(new UUID("user-0-0")));
        assertArrayEquals(data, dataStorage.get(id));

        assertTrue(peer.deleteAccount(username, password));
        assertNull(dataStorage.get(new UUID("user-0-0")));
        assertNull(dataStorage.get(id));

        UserMetadata userMetadata = peer.authenticate(username, password);
        assertEquals(new HashSet<>(), userMetadata.getFiles());

        peer.leave();
    }

    @Test(timeout=1000)
    public void deleteAccount_10peer() throws Exception {
        int KEY_SIZE = 10;

        int[] ids = { 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000 };

        Peer[] peers = new Peer[ids.length];
        for(int i = 0; i < ids.length; ++i)
            peers[i] = new Peer(KEY_SIZE, ids[i], InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peers[0].join());
        for(int i = 1; i < ids.length; ++i)
            peers[i].join(peers[0].getSocketAddress());

        Username username = new Username("user");
        Password password = new Password("1234");

        Main.Path path = new Main.Path("data");
        Main.File file = new Main.File(username, path, 1, 1);
        UUID id = file.getChunk(0).getReplica(0).getUUID();
        byte[] data = ("my data").getBytes();

        assertTrue(peers[0].backup(username, password, path, 1, new ByteArrayChunkIterator(data, Main.CHUNK_SIZE)));
        assertNotNull(peers[9].getDataStorage().get(new UUID("user-0-0")));
        assertArrayEquals(data, peers[9].getDataStorage().get(id));

        assertTrue(peers[0].deleteAccount(username, password));
        assertNull(peers[9].getDataStorage().get(new UUID("user-0-0")));
        assertNull(peers[9].getDataStorage().get(id));

        UserMetadata userMetadata = peers[2].authenticate(username, password);
        assertEquals(new HashSet<>(), userMetadata.getFiles());

        for(Peer p: peers) p.leave();
    }

}
