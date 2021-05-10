package sdis.Modules.DataStorage;

import org.junit.Test;
import sdis.Peer;
import sdis.UUID;

import java.net.InetAddress;

import static org.junit.Assert.*;

public class DataStorageTest {

    @Test(timeout=10000)
    public void hasSpace_1peer() throws Exception {
        Peer peer1 = new Peer(8, 0, InetAddress.getByName("localhost"));
        peer1.join().get();

        UUID id = new UUID("1234567890-0-1");
        byte[] data = "my data".getBytes();

        DataStorage dataStorage1 = peer1.getDataStorage();
        LocalDataStorage localDataStorage1 = dataStorage1.getLocalDataStorage();

        assertFalse(localDataStorage1.has(id).get());
        assertTrue(localDataStorage1.canPut(7).get());
        assertFalse(dataStorage1.successorHasStored(id));

        peer1.leave().get();
    }

    @Test(timeout=10000)
    public void get_put_1peer() throws Exception {
        Peer peer1 = new Peer(8, 0, InetAddress.getByName("localhost"));
        peer1.join().get();

        UUID id = new UUID("1234567890-0-1");
        byte[] data = "my data".getBytes();

        DataStorage dataStorage1 = peer1.getDataStorage();
        LocalDataStorage localDataStorage1 = dataStorage1.getLocalDataStorage();

        assertEquals(localDataStorage1.getMemoryUsed().get().intValue(), 0);
        assertTrue(localDataStorage1.canPut(7).get());
        assertFalse(localDataStorage1.has(id).get());
        assertNull(localDataStorage1.get(id).get());
        assertNull(dataStorage1.get(id).get());
        assertFalse(dataStorage1.successorHasStored(id));

        assertTrue(dataStorage1.put(id, data).get());

        assertEquals(localDataStorage1.getMemoryUsed().get().intValue(), 7);
        assertTrue(localDataStorage1.canPut(7).get());
        assertTrue(localDataStorage1.has(id).get());
        assertArrayEquals(localDataStorage1.get(id).get(), data);
        assertArrayEquals(dataStorage1.get(id).get(), data);
        assertFalse(dataStorage1.successorHasStored(id));

        peer1.leave().get();
    }

    @Test(timeout=10000)
    public void delete_1peer() throws Exception {
        Peer peer1 = new Peer(8, 0, InetAddress.getByName("localhost"));
        peer1.join().get();

        UUID id = new UUID("1234567890-0-1");
        byte[] data = "my data".getBytes();

        DataStorage dataStorage1 = peer1.getDataStorage();
        LocalDataStorage localDataStorage1 = dataStorage1.getLocalDataStorage();

        assertTrue(dataStorage1.put(id, data).get());

        assertTrue(dataStorage1.delete(id).get());

        assertEquals(localDataStorage1.getMemoryUsed().get().intValue(), 0);
        assertTrue(localDataStorage1.canPut(7).get());
        assertFalse(localDataStorage1.has(id).get());
        assertNull(localDataStorage1.get(id).get());
        assertNull(dataStorage1.get(id).get());
        assertFalse(dataStorage1.successorHasStored(id));

        peer1.leave().get();
    }

    @Test(timeout=10000)
    public void put_retry_1peer() throws Exception {
        Peer peer1 = new Peer(8, 0, InetAddress.getByName("localhost"));
        peer1.join().get();

        UUID id = new UUID("1234567890-0-1");
        byte[] data = "my data".getBytes();

        DataStorage dataStorage1 = peer1.getDataStorage();
        LocalDataStorage localDataStorage1 = dataStorage1.getLocalDataStorage();

        assertTrue(dataStorage1.put(id, data).get());
        assertArrayEquals(dataStorage1.get(id).get(), data);

        assertTrue(dataStorage1.put(id, data).get());

        assertEquals(localDataStorage1.getMemoryUsed().get().intValue(), 7);
        assertTrue(localDataStorage1.canPut(7).get());
        assertTrue(localDataStorage1.has(id).get());
        assertArrayEquals(localDataStorage1.get(id).get(), data);
        assertArrayEquals(dataStorage1.get(id).get(), data);
        assertFalse(dataStorage1.successorHasStored(id));

        peer1.leave().get();
    }

    @Test(timeout=10000)
    public void put_get_2peer() throws Exception {
        Peer peer1 = new Peer(8, 0, InetAddress.getByName("localhost"));
        peer1.join().get();

        Peer peer2 = new Peer(8, 10, InetAddress.getByName("localhost"));
        peer2.join(peer1.getSocketAddress()).get();

        UUID id = new UUID("1234567890-0-1");
        byte[] data = "my data".getBytes();

        DataStorage dataStorage1 = peer1.getDataStorage();
        LocalDataStorage localDataStorage1 = dataStorage1.getLocalDataStorage();

        DataStorage dataStorage2 = peer2.getDataStorage();
        LocalDataStorage localDataStorage2 = dataStorage2.getLocalDataStorage();

        localDataStorage1.setCapacity(0);

        assertTrue(dataStorage1.put(id, data).get());

        assertEquals(localDataStorage1.getMemoryUsed().get().intValue(), 0);
        assertFalse(localDataStorage1.canPut(7).get());
        assertFalse(localDataStorage1.has(id).get());
        assertNull(localDataStorage1.get(id).get());
        assertArrayEquals(dataStorage1.get(id).get(), data);
        assertTrue(dataStorage1.successorHasStored(id));

        assertEquals(localDataStorage2.getMemoryUsed().get().intValue(), 7);
        assertTrue(localDataStorage2.canPut(7).get());
        assertTrue(localDataStorage2.has(id).get());
        assertArrayEquals(localDataStorage2.get(id).get(), data);
        assertArrayEquals(dataStorage2.get(id).get(), data);
        assertFalse(dataStorage2.successorHasStored(id));

        peer1.leave().get();
        peer2.leave().get();
    }

    @Test(timeout=10000)
    public void delete_2peer() throws Exception {
        Peer peer1 = new Peer(8, 0, InetAddress.getByName("localhost"));
        peer1.join().get();

        Peer peer2 = new Peer(8, 10, InetAddress.getByName("localhost"));
        peer2.join(peer1.getSocketAddress()).get();

        UUID id = new UUID("1234567890-0-1");
        byte[] data = "my data".getBytes();

        DataStorage dataStorage1 = peer1.getDataStorage();
        LocalDataStorage localDataStorage1 = dataStorage1.getLocalDataStorage();

        DataStorage dataStorage2 = peer2.getDataStorage();
        LocalDataStorage localDataStorage2 = dataStorage2.getLocalDataStorage();

        localDataStorage1.setCapacity(0);

        assertTrue(dataStorage1.put(id, data).get());

        assertTrue(dataStorage1.delete(id).get());

        assertEquals(localDataStorage1.getMemoryUsed().get().intValue(), 0);
        assertFalse(localDataStorage1.canPut(7).get());
        assertFalse(localDataStorage1.has(id).get());
        assertNull(localDataStorage1.get(id).get());
        assertNull(dataStorage1.get(id).get());
        assertFalse(dataStorage1.successorHasStored(id));

        assertEquals(localDataStorage2.getMemoryUsed().get().intValue(), 0);
        assertTrue(localDataStorage2.canPut(7).get());
        assertFalse(localDataStorage2.has(id).get());
        assertNull(localDataStorage2.get(id).get());
        assertNull(dataStorage2.get(id).get());
        assertFalse(dataStorage2.successorHasStored(id));

        peer1.leave().get();
    }
}
