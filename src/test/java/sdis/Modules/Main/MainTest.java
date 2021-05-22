package sdis.Modules.Main;

import org.junit.Test;
import sdis.Peer;
import sdis.Storage.ChunkOutput;
import sdis.Storage.DataBuilderChunkOutput;
import sdis.Utils.DataBuilder;

import java.net.InetAddress;
import java.nio.file.Paths;
import java.util.HashSet;

import static org.junit.Assert.*;

public class MainTest {

    @Test(timeout=10000)
    public void backupFileProtocol_1peer() throws Exception {
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
    public void backupFileProtocol_10peer() throws Exception {
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

        Username username = new Username("user1");
        Password password = new Password("1234");
        UserMetadata userMetadata = peers[0].authenticate(username, password);

        assertNotNull(userMetadata);
        assertEquals(username, userMetadata.getUsername());
        assertEquals(password, userMetadata.getPassword());
        assertEquals(new HashSet<Main.Path>(), userMetadata.getFiles());

        for(Peer p: peers) p.leave().get();
    }

}
