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

        DataStorage dataStorage1 = peer1.getDataStorage();
        LocalDataStorage localDataStorage1 = dataStorage1.getLocalDataStorage();
        boolean hasStored = localDataStorage1.has(new UUID("1234567890-0-1")).get();
        boolean hasSpace = localDataStorage1.canPut(7).get();
        boolean pointsToSuccessor = dataStorage1.successorHasStored(new UUID("1234567890-0-1"));

        assertFalse(hasStored);
        assertTrue(hasSpace);
        assertFalse(pointsToSuccessor);

        peer1.leave().get();
    }

    @Test(timeout=10000)
    public void put_1peer() throws Exception {
        Peer peer1 = new Peer(8, 0, InetAddress.getByName("localhost"));
        peer1.join().get();

        UUID id = new UUID("1234567890-0-1");
        byte[] data = "my data".getBytes();

        DataStorage dataStorage1 = peer1.getDataStorage();
        LocalDataStorage localDataStorage1 = dataStorage1.getLocalDataStorage();

        assertTrue(dataStorage1.put(id, data).get());
        assertTrue(localDataStorage1.has(id).get());
        assertNotNull(dataStorage1.get(id).get());
        assertEquals(localDataStorage1.getMemoryUsed().get().intValue(), 7);

        peer1.leave().get();
    }

    @Test(timeout=10000)
    public void get_1peer() throws Exception {
        Peer peer1 = new Peer(8, 0, InetAddress.getByName("localhost"));
        peer1.join().get();

        DataStorage dataStorage1 = peer1.getDataStorage();
        assertTrue(dataStorage1.put(new UUID("1234567890-0-1"), "my data".getBytes()).get());
        assertArrayEquals(dataStorage1.get(new UUID("1234567890-0-1")).get(), "my data".getBytes());

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
        assertArrayEquals(dataStorage1.get(id).get(), data);
        assertTrue(dataStorage1.delete(id).get());
        assertFalse(localDataStorage1.has(id).get());
        assertNull(dataStorage1.get(id).get());
        assertEquals(localDataStorage1.getMemoryUsed().get().intValue(), 0);

        peer1.leave().get();
    }

    @Test(timeout=10000)
    public void put_retry_1peer() throws Exception {
        Peer peer1 = new Peer(8, 0, InetAddress.getByName("localhost"));
        peer1.join().get();

        DataStorage dataStorage1 = peer1.getDataStorage();

        assertTrue(dataStorage1.put(new UUID("1234567890-0-1"), "my data".getBytes()).get());
        assertArrayEquals(dataStorage1.get(new UUID("1234567890-0-1")).get(), "my data".getBytes());

        assertTrue(dataStorage1.put(new UUID("1234567890-0-1"), "my data".getBytes()).get());
        assertArrayEquals(dataStorage1.get(new UUID("1234567890-0-1")).get(), "my data".getBytes());

        peer1.leave().get();
    }
}
