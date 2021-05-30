package sdis.Modules.SystemStorage;

import org.junit.Test;
import sdis.Modules.DataStorage.DataStorage;
import sdis.Modules.DataStorage.LocalDataStorage;
import sdis.Peer;
import sdis.UUID;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.util.HashSet;

import static org.junit.Assert.*;

public class SystemStorageTest {

    @Test(timeout=1000)
    public void put_get_1peer() throws Exception {
        int KEY_SIZE = 10;

        Peer peer1 = new Peer(KEY_SIZE, 0, InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peer1.join());

        UUID id = new UUID("1234567890-0-1");
        byte[] data = "my data".getBytes();

        SystemStorage systemStorage1 = peer1.getSystemStorage();

        assertTrue(systemStorage1.put(id, data));
        assertArrayEquals(data, systemStorage1.get(id));

        peer1.leave();
    }

    @Test(timeout=1000)
    public void delete_1peer() throws Exception {
        int KEY_SIZE = 10;

        Peer peer1 = new Peer(KEY_SIZE, 0, InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peer1.join());

        UUID id = new UUID("1234567890-0-1");
        byte[] data = "my data".getBytes();

        SystemStorage systemStorage1 = peer1.getSystemStorage();

        assertTrue(systemStorage1.put(id, data));
        assertArrayEquals(data, systemStorage1.get(id));
        assertTrue(systemStorage1.delete(id));
        assertNull(systemStorage1.get(id));

        peer1.leave();
    }

    @Test(timeout=1000)
    public void put_get_10peer() throws Exception {
        int KEY_SIZE = 10;

        int[] ids = { 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000 };
        Peer[] peers = new Peer[ids.length];
        for(int i = 0; i < ids.length; ++i)
            peers[i] = new Peer(KEY_SIZE, ids[i], InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peers[0].join());
        InetSocketAddress address = peers[0].getSocketAddress();
        for(int i = 1; i < ids.length; ++i){
            peers[i].join(address);
        }

        UUID id = new UUID("0123456789-0-1");
        byte[] data = "my data".getBytes();

        assertTrue(peers[0].getSystemStorage().put(id, data));
        for(int i = 0; i < peers.length; ++i)
            assertArrayEquals(data, peers[0].getSystemStorage().get(id));

        DataStorage dataStorage = peers[2].getDataStorage();
        LocalDataStorage localDataStorage = dataStorage.getLocalDataStorage();

        assertEquals(7, localDataStorage.getMemoryUsed());
        assertTrue(localDataStorage.canPut(7));
        assertEquals(new HashSet<UUID>(){{ add(id); }}, localDataStorage.getAll());
        assertTrue(localDataStorage.has(id));
        assertArrayEquals(data, localDataStorage.get(id));

        assertEquals(new HashSet<>(), dataStorage.getRedirects());
        assertEquals(new HashSet<UUID>(){{ add(id); }}, dataStorage.getAll());
        assertTrue(dataStorage.has(id));
        assertArrayEquals(data, dataStorage.get(id));
        assertFalse(dataStorage.successorHasStored(id));

        for(int i : new int[]{0, 1, 3, 4, 5, 6, 7, 8, 9}) {
            assertNull(peers[i].getDataStorage().get(id));

            dataStorage = peers[i].getDataStorage();
            localDataStorage = dataStorage.getLocalDataStorage();

            assertEquals(0, localDataStorage.getMemoryUsed());
            assertTrue(localDataStorage.canPut(7));
            assertEquals(new HashSet<UUID>(), localDataStorage.getAll());
            assertFalse(localDataStorage.has(id));
            assertNull(localDataStorage.get(id));

            assertEquals(new HashSet<>(), dataStorage.getRedirects());
            assertEquals(new HashSet<UUID>(), dataStorage.getAll());
            assertFalse(dataStorage.has(id));
            assertNull(dataStorage.get(id));
            assertFalse(dataStorage.successorHasStored(id));
        }

        for(Peer p: peers) p.leave();
    }

    @Test(timeout=1000)
    public void put_get_10peer_lessMemory() throws Exception {
        int KEY_SIZE = 10;

        int[] ids = { 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000 };
        Peer[] peers = new Peer[ids.length];
        for(int i = 0; i < ids.length; ++i)
            peers[i] = new Peer(KEY_SIZE, ids[i], InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peers[0].join());
        InetSocketAddress address = peers[0].getSocketAddress();
        for(int i = 1; i < ids.length; ++i){
            peers[i].join(address);
        }

        peers[2].getDataStorage().getLocalDataStorage().setCapacity(0);

        UUID id = new UUID("0123456789-0-1");
        byte[] data = "my data".getBytes();

        assertTrue(peers[0].getSystemStorage().put(id, data));
        for(int i = 0; i < peers.length; ++i)
            assertArrayEquals(data, peers[0].getSystemStorage().get(id));

        DataStorage dataStorage = peers[2].getDataStorage();
        LocalDataStorage localDataStorage = dataStorage.getLocalDataStorage();

        assertEquals(0, localDataStorage.getMemoryUsed());
        assertFalse(localDataStorage.canPut(7));
        assertEquals(new HashSet<UUID>(), localDataStorage.getAll());
        assertFalse(localDataStorage.has(id));
        assertNull(localDataStorage.get(id));

        assertEquals(new HashSet<>(){{ add(id); }}, dataStorage.getRedirects());
        assertEquals(new HashSet<UUID>(){{ add(id); }}, dataStorage.getAll());
        assertTrue(dataStorage.has(id));
        assertArrayEquals(data, dataStorage.get(id));
        assertTrue(dataStorage.successorHasStored(id));

        dataStorage = peers[3].getDataStorage();
        localDataStorage = dataStorage.getLocalDataStorage();

        assertEquals(7, localDataStorage.getMemoryUsed());
        assertTrue(localDataStorage.canPut(7));
        assertEquals(new HashSet<UUID>(){{ add(id); }}, localDataStorage.getAll());
        assertTrue(localDataStorage.has(id));
        assertArrayEquals(data, localDataStorage.get(id));

        assertEquals(new HashSet<>(), dataStorage.getRedirects());
        assertEquals(new HashSet<UUID>(), dataStorage.getAll());
        assertFalse(dataStorage.has(id));
        assertNull(dataStorage.get(id));
        assertFalse(dataStorage.successorHasStored(id));

        for(int i : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}) {
            if(i != 2) assertNull(peers[i].getDataStorage().get(id));

            dataStorage = peers[i].getDataStorage();
            localDataStorage = dataStorage.getLocalDataStorage();

            if(i != 3) assertEquals(0, localDataStorage.getMemoryUsed());
            if(i != 2) assertTrue(localDataStorage.canPut(7));
            if(i != 3) assertEquals(new HashSet<UUID>(), localDataStorage.getAll());
            if(i != 3) assertFalse(localDataStorage.has(id));
            if(i != 3) assertNull(localDataStorage.get(id));

            if(i != 2) assertEquals(new HashSet<>(), dataStorage.getRedirects());
            if(i != 2) assertEquals(new HashSet<UUID>(), dataStorage.getAll());
            if(i != 2) assertFalse(dataStorage.has(id));
            if(i != 2) assertNull(dataStorage.get(id));
            if(i != 2) assertFalse(dataStorage.successorHasStored(id));
        }

        for(Peer p: peers) p.leave();
    }

    @Test(timeout=1000)
    public void delete_10peer() throws Exception {
        int KEY_SIZE = 10;

        int[] ids = { 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000 };
        Peer[] peers = new Peer[ids.length];
        for(int i = 0; i < ids.length; ++i)
            peers[i] = new Peer(KEY_SIZE, ids[i], InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peers[0].join());
        InetSocketAddress address = peers[0].getSocketAddress();
        for(int i = 1; i < ids.length; ++i){
            peers[i].join(address);
        }

        UUID id = new UUID("0123456789-0-1");
        byte[] data = "my data".getBytes();

        assertTrue(peers[0].getSystemStorage().put(id, data));
        assertArrayEquals(data, peers[2].getDataStorage().get(id));
        assertTrue(peers[0].getSystemStorage().delete(id));

        for(int i = 1; i < peers.length; ++i)
            assertNull(peers[0].getSystemStorage().get(id));

        for(int i : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}) {
            assertNull(peers[i].getDataStorage().get(id));

            DataStorage dataStorage = peers[i].getDataStorage();
            LocalDataStorage localDataStorage = dataStorage.getLocalDataStorage();

            assertEquals(0, localDataStorage.getMemoryUsed());
            assertTrue(localDataStorage.canPut(7));
            assertEquals(new HashSet<UUID>(), localDataStorage.getAll());
            assertFalse(localDataStorage.has(id));
            assertNull(localDataStorage.get(id));

            assertEquals(new HashSet<>(), dataStorage.getRedirects());
            assertEquals(new HashSet<UUID>(), dataStorage.getAll());
            assertFalse(dataStorage.has(id));
            assertNull(dataStorage.get(id));
            assertFalse(dataStorage.successorHasStored(id));
        }

        assertFalse(peers[0].getSystemStorage().delete(id));

        for(Peer p: peers) p.leave();
    }

    @Test(timeout=1000)
    public void reclaim_10peer() throws Exception {
        int KEY_SIZE = 10;

        int[] ids = { 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000 };
        Peer[] peers = new Peer[ids.length];
        for(int i = 0; i < ids.length; ++i)
            peers[i] = new Peer(KEY_SIZE, ids[i], InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peers[0].join());
        InetSocketAddress address = peers[0].getSocketAddress();
        for(int i = 1; i < ids.length; ++i){
            peers[i].join(address);
        }



        UUID id = new UUID("0123456789-0-1");
        byte[] data = "my data".getBytes();

        assertTrue(peers[0].getSystemStorage().put(id, data));

        assertTrue(peers[2].reclaim(0));

        for(int i = 0; i < peers.length; ++i)
            assertArrayEquals(data, peers[0].getSystemStorage().get(id));

        DataStorage dataStorage = peers[2].getDataStorage();
        LocalDataStorage localDataStorage = dataStorage.getLocalDataStorage();

        assertEquals(0, localDataStorage.getMemoryUsed());
        assertFalse(localDataStorage.canPut(7));
        assertEquals(new HashSet<UUID>(), localDataStorage.getAll());
        assertFalse(localDataStorage.has(id));
        assertNull(localDataStorage.get(id));

        assertEquals(new HashSet<>(){{ add(id); }}, dataStorage.getRedirects());
        assertEquals(new HashSet<UUID>(){{ add(id); }}, dataStorage.getAll());
        assertTrue(dataStorage.has(id));
        assertArrayEquals(data, dataStorage.get(id));
        assertTrue(dataStorage.successorHasStored(id));

        dataStorage = peers[3].getDataStorage();
        localDataStorage = dataStorage.getLocalDataStorage();

        assertEquals(7, localDataStorage.getMemoryUsed());
        assertTrue(localDataStorage.canPut(7));
        assertEquals(new HashSet<UUID>(){{ add(id); }}, localDataStorage.getAll());
        assertTrue(localDataStorage.has(id));
        assertArrayEquals(data, localDataStorage.get(id));

        assertEquals(new HashSet<>(), dataStorage.getRedirects());
        assertEquals(new HashSet<UUID>(), dataStorage.getAll());
        assertFalse(dataStorage.has(id));
        assertNull(dataStorage.get(id));
        assertFalse(dataStorage.successorHasStored(id));

        for(int i : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}) {
            if(i != 2) assertNull(peers[i].getDataStorage().get(id));

            dataStorage = peers[i].getDataStorage();
            localDataStorage = dataStorage.getLocalDataStorage();

            if(i != 3) assertEquals(0, localDataStorage.getMemoryUsed());
            if(i != 2) assertTrue(localDataStorage.canPut(7));
            if(i != 3) assertEquals(new HashSet<UUID>(), localDataStorage.getAll());
            if(i != 3) assertFalse(localDataStorage.has(id));
            if(i != 3) assertNull(localDataStorage.get(id));

            if(i != 2) assertEquals(new HashSet<>(), dataStorage.getRedirects());
            if(i != 2) assertEquals(new HashSet<UUID>(), dataStorage.getAll());
            if(i != 2) assertFalse(dataStorage.has(id));
            if(i != 2) assertNull(dataStorage.get(id));
            if(i != 2) assertFalse(dataStorage.successorHasStored(id));
        }

        for(Peer p: peers) p.leave();
    }

}
