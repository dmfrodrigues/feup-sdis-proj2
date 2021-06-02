package sdis.Modules.Main;

import org.junit.Test;
import sdis.Peer;
import sdis.Storage.ByteArrayChunkIterator;
import sdis.Storage.DataBuilderChunkOutput;
import sdis.UUID;
import sdis.Utils.DataBuilder;

import java.net.InetAddress;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class MainLargeFilesTest {

    @Test(timeout=1000)
    public void backup_1peers_large_0() throws Exception {
        int KEY_SIZE = 10;

        Peer peer1 = new Peer(KEY_SIZE, 0, InetAddress.getByName("localhost"), Paths.get("bin"));

        assertTrue(peer1.join());

        Username username = new Username("user");
        Password password = new Password("1234");

        Main.Path path1 = new Main.Path("data1");
        byte[] data1 = new byte[63999];
        for(int i = 0; i < data1.length; ++i)
            data1[i] = Integer.toString(i % 10).getBytes()[0];

        assertTrue(peer1.backup(username, password, path1, 1, new ByteArrayChunkIterator(data1, Main.CHUNK_SIZE)));

        assertArrayEquals(data1, peer1.getSystemStorage().getDataStorage().getLocalDataStorage().get(new UUID("user/data1-0-0")));

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
        byte[] data2 = new byte[700000];
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
    public void backup_1peers_large_4() throws Exception {
        int KEY_SIZE = 10;

        Peer peer1 = new Peer(KEY_SIZE, 0, InetAddress.getByName("localhost"), Paths.get("bin"));

        assertTrue(peer1.join());

        Username username = new Username("user");
        Password password = new Password("1234");

        Main.Path path2 = new Main.Path("data2");
        byte[] data2 = new byte[704];
        for(int i = 0; i < data2.length; ++i)
            data2[i] = Integer.toString(i % 9).getBytes()[0];

        assertTrue(peer1.backup(username, password, path2, 1, new ByteArrayChunkIterator(data2, Main.CHUNK_SIZE)));
        DataBuilder builder = new DataBuilder();
        DataBuilderChunkOutput chunkOutput = new DataBuilderChunkOutput(builder, 10);
        assertTrue(peer1.restore(username, password, path2, chunkOutput));
        assertArrayEquals(data2, builder.get());

        peer1.leave();
    }
}
