package sdis.Modules.SystemStorage;

import org.junit.Test;
import sdis.Modules.DataStorage.DataStorage;
import sdis.Modules.DataStorage.LocalDataStorage;
import sdis.Peer;
import sdis.UUID;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashSet;

import static org.junit.Assert.*;

public class SystemStorageTest {

    @Test(timeout=10000)
    public void put_get_1peer() throws Exception {
        int KEY_SIZE = 10;

        Peer peer1 = new Peer(KEY_SIZE, 0, InetAddress.getByName("localhost"));
        peer1.join().get();

        UUID id = new UUID("1234567890-0-1");
        byte[] data = "my data".getBytes();

        SystemStorage systemStorage1 = peer1.getSystemStorage();

        assertTrue(systemStorage1.put(id, data).get());
        assertArrayEquals(data, systemStorage1.get(id).get());

        peer1.leave().get();
    }

    @Test(timeout=10000)
    public void delete_1peer() throws Exception {
        int KEY_SIZE = 10;

        Peer peer1 = new Peer(KEY_SIZE, 0, InetAddress.getByName("localhost"));
        peer1.join().get();

        UUID id = new UUID("1234567890-0-1");
        byte[] data = "my data".getBytes();

        SystemStorage systemStorage1 = peer1.getSystemStorage();

        assertTrue(systemStorage1.put(id, data).get());
        assertArrayEquals(data, systemStorage1.get(id).get());
        assertTrue(systemStorage1.delete(id).get());
        assertNull(systemStorage1.get(id).get());

        peer1.leave().get();
    }

    @Test
    public void put_get_10peer() throws Exception {
        int KEY_SIZE = 10;

        int[] ids = { 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000 };
        Peer[] peers = new Peer[ids.length];
        for(int i = 0; i < ids.length; ++i)
            peers[i] = new Peer(KEY_SIZE, ids[i], InetAddress.getByName("localhost"));
        peers[0].join().get();
        InetSocketAddress address = peers[0].getSocketAddress();
        for(int i = 1; i < ids.length; ++i){
            peers[i].join(address).get();
        }

        UUID id = new UUID("0123456789-0-1");
        byte[] data = "my data".getBytes();

        assertTrue(peers[0].getSystemStorage().put(id, data).get());
        for(int i = 0; i < peers.length; ++i)
            assertArrayEquals(data, peers[0].getSystemStorage().get(id).get());

        DataStorage dataStorage = peers[5].getDataStorage();
        LocalDataStorage localDataStorage = dataStorage.getLocalDataStorage();

        assertEquals(7, localDataStorage.getMemoryUsed().get().intValue());
        assertTrue(localDataStorage.canPut(7).get());
        assertEquals(new HashSet<UUID>(){{ add(id); }}, localDataStorage.getAll());
        assertTrue(localDataStorage.has(id));
        assertArrayEquals(data, localDataStorage.get(id).get());

        assertEquals(new HashSet<>(), dataStorage.getRedirects());
        assertEquals(new HashSet<UUID>(){{ add(id); }}, dataStorage.getAll());
        assertTrue(dataStorage.has(id));
        assertArrayEquals(data, dataStorage.get(id).get());
        assertFalse(dataStorage.successorHasStored(id));

        for(int i : new int[]{0, 1, 2, 3, 4, 6, 7, 8, 9}) {
            assertNull(peers[i].getDataStorage().get(id).get());

            dataStorage = peers[i].getDataStorage();
            localDataStorage = dataStorage.getLocalDataStorage();

            assertEquals(0, localDataStorage.getMemoryUsed().get().intValue());
            assertTrue(localDataStorage.canPut(7).get());
            assertEquals(new HashSet<UUID>(), localDataStorage.getAll());
            assertFalse(localDataStorage.has(id));
            assertNull(localDataStorage.get(id).get());

            assertEquals(new HashSet<>(), dataStorage.getRedirects());
            assertEquals(new HashSet<UUID>(), dataStorage.getAll());
            assertFalse(dataStorage.has(id));
            assertNull(dataStorage.get(id).get());
            assertFalse(dataStorage.successorHasStored(id));
        }

        for(Peer p: peers) p.leave().get();
    }

    @Test
    public void put_get_10peer_lessMemory() throws Exception {
        int KEY_SIZE = 10;

        int[] ids = { 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000 };
        Peer[] peers = new Peer[ids.length];
        for(int i = 0; i < ids.length; ++i)
            peers[i] = new Peer(KEY_SIZE, ids[i], InetAddress.getByName("localhost"));
        peers[0].join().get();
        InetSocketAddress address = peers[0].getSocketAddress();
        for(int i = 1; i < ids.length; ++i){
            peers[i].join(address).get();
        }

        peers[5].getDataStorage().getLocalDataStorage().setCapacity(0);

        UUID id = new UUID("0123456789-0-1");
        byte[] data = "my data".getBytes();

        assertTrue(peers[0].getSystemStorage().put(id, data).get());
        for(int i = 0; i < peers.length; ++i)
            assertArrayEquals(data, peers[0].getSystemStorage().get(id).get());

        DataStorage dataStorage = peers[5].getDataStorage();
        LocalDataStorage localDataStorage = dataStorage.getLocalDataStorage();

        assertEquals(0, localDataStorage.getMemoryUsed().get().intValue());
        assertFalse(localDataStorage.canPut(7).get());
        assertEquals(new HashSet<UUID>(), localDataStorage.getAll());
        assertFalse(localDataStorage.has(id));
        assertNull(localDataStorage.get(id).get());

        assertEquals(new HashSet<>(){{ add(id); }}, dataStorage.getRedirects());
        assertEquals(new HashSet<UUID>(){{ add(id); }}, dataStorage.getAll());
        assertTrue(dataStorage.has(id));
        assertArrayEquals(data, dataStorage.get(id).get());
        assertTrue(dataStorage.successorHasStored(id));

        dataStorage = peers[6].getDataStorage();
        localDataStorage = dataStorage.getLocalDataStorage();

        assertEquals(7, localDataStorage.getMemoryUsed().get().intValue());
        assertTrue(localDataStorage.canPut(7).get());
        assertEquals(new HashSet<UUID>(){{ add(id); }}, localDataStorage.getAll());
        assertTrue(localDataStorage.has(id));
        assertArrayEquals(data, localDataStorage.get(id).get());

        assertEquals(new HashSet<>(), dataStorage.getRedirects());
        assertEquals(new HashSet<UUID>(), dataStorage.getAll());
        assertFalse(dataStorage.has(id));
        assertNull(dataStorage.get(id).get());
        assertFalse(dataStorage.successorHasStored(id));

        for(int i : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}) {
            if(i != 5) assertNull(peers[i].getDataStorage().get(id).get());

            dataStorage = peers[i].getDataStorage();
            localDataStorage = dataStorage.getLocalDataStorage();

            if(i != 6) assertEquals(0, localDataStorage.getMemoryUsed().get().intValue());
            if(i != 5) assertTrue(localDataStorage.canPut(7).get());
            if(i != 6) assertEquals(new HashSet<UUID>(), localDataStorage.getAll());
            if(i != 6) assertFalse(localDataStorage.has(id));
            if(i != 6) assertNull(localDataStorage.get(id).get());

            if(i != 5) assertEquals(new HashSet<>(), dataStorage.getRedirects());
            if(i != 5) assertEquals(new HashSet<UUID>(), dataStorage.getAll());
            if(i != 5) assertFalse(dataStorage.has(id));
            if(i != 5) assertNull(dataStorage.get(id).get());
            if(i != 5) assertFalse(dataStorage.successorHasStored(id));
        }

        for(Peer p: peers) p.leave().get();
    }

    @Test
    public void delete_10peer() throws Exception {
        int KEY_SIZE = 10;

        int[] ids = { 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000 };
        Peer[] peers = new Peer[ids.length];
        for(int i = 0; i < ids.length; ++i)
            peers[i] = new Peer(KEY_SIZE, ids[i], InetAddress.getByName("localhost"));
        peers[0].join().get();
        InetSocketAddress address = peers[0].getSocketAddress();
        for(int i = 1; i < ids.length; ++i){
            peers[i].join(address).get();
        }

        UUID id = new UUID("0123456789-0-1");
        byte[] data = "my data".getBytes();

        assertTrue(peers[0].getSystemStorage().put(id, data).get());
        assertArrayEquals(data, peers[5].getDataStorage().get(id).get());
        assertTrue(peers[0].getSystemStorage().delete(id).get());

        for(int i = 1; i < peers.length; ++i)
            assertNull(peers[0].getSystemStorage().get(id).get());

        for(int i : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}) {
            assertNull(peers[i].getDataStorage().get(id).get());

            DataStorage dataStorage = peers[i].getDataStorage();
            LocalDataStorage localDataStorage = dataStorage.getLocalDataStorage();

            assertEquals(0, localDataStorage.getMemoryUsed().get().intValue());
            assertTrue(localDataStorage.canPut(7).get());
            assertEquals(new HashSet<UUID>(), localDataStorage.getAll());
            assertFalse(localDataStorage.has(id));
            assertNull(localDataStorage.get(id).get());

            assertEquals(new HashSet<>(), dataStorage.getRedirects());
            assertEquals(new HashSet<UUID>(), dataStorage.getAll());
            assertFalse(dataStorage.has(id));
            assertNull(dataStorage.get(id).get());
            assertFalse(dataStorage.successorHasStored(id));
        }

        assertFalse(peers[0].getSystemStorage().delete(id).get());

        for(Peer p: peers) p.leave().get();
    }

}
