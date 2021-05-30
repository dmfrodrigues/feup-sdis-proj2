package sdis.Modules.DataStorage;

import org.junit.Test;
import sdis.Modules.Chord.Chord;
import sdis.Peer;
import sdis.UUID;

import java.net.InetAddress;
import java.nio.file.Paths;
import java.util.HashSet;

import static org.junit.Assert.*;

public class DataStorageTest {

    @Test(timeout=1000)
    public void put_get_1peer() throws Exception {
        Peer peer1 = new Peer(8, 0, InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peer1.join());

        UUID id = new UUID("1234567890-0-1");
        byte[] data = "my data".getBytes();

        DataStorage dataStorage1 = peer1.getDataStorage();
        LocalDataStorage localDataStorage1 = dataStorage1.getLocalDataStorage();

        assertEquals(0, localDataStorage1.getMemoryUsed());
        assertTrue(localDataStorage1.canPut(7));
        assertEquals(new HashSet<UUID>(), localDataStorage1.getAll());
        assertFalse(localDataStorage1.has(id));
        assertNull(localDataStorage1.get(id));

        assertEquals(new HashSet<>(), dataStorage1.getRedirects());
        assertEquals(new HashSet<UUID>(), dataStorage1.getAll());
        assertFalse(dataStorage1.has(id));
        assertNull(dataStorage1.get(id));
        assertFalse(dataStorage1.successorHasStored(id));

        assertTrue(dataStorage1.put(id, data));

        assertEquals(7, localDataStorage1.getMemoryUsed());
        assertTrue(localDataStorage1.canPut(7));
        assertEquals(new HashSet<UUID>(){{ add(id); }}, localDataStorage1.getAll());
        assertTrue(localDataStorage1.has(id));
        assertArrayEquals(data, localDataStorage1.get(id));

        assertEquals(new HashSet<>(), dataStorage1.getRedirects());
        assertEquals(new HashSet<UUID>(){{ add(id); }}, dataStorage1.getAll());
        assertTrue(dataStorage1.has(id));
        assertArrayEquals(dataStorage1.get(id), data);
        assertFalse(dataStorage1.successorHasStored(id));

        peer1.leave();
    }

    @Test(timeout=1000)
    public void delete_1peer() throws Exception {
        Peer peer1 = new Peer(8, 0, InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peer1.join());

        UUID id = new UUID("1234567890-0-1");
        byte[] data = "my data".getBytes();

        DataStorage dataStorage1 = peer1.getDataStorage();
        LocalDataStorage localDataStorage1 = dataStorage1.getLocalDataStorage();

        assertTrue(dataStorage1.put(id, data));

        assertTrue(dataStorage1.delete(id));

        assertEquals(0, localDataStorage1.getMemoryUsed());
        assertTrue(localDataStorage1.canPut(7));
        assertEquals(new HashSet<UUID>(), localDataStorage1.getAll());
        assertFalse(localDataStorage1.has(id));
        assertNull(localDataStorage1.get(id));

        assertEquals(new HashSet<>(), dataStorage1.getRedirects());
        assertEquals(new HashSet<UUID>(), dataStorage1.getAll());
        assertFalse(dataStorage1.has(id));
        assertNull(dataStorage1.get(id));
        assertFalse(dataStorage1.successorHasStored(id));

        peer1.leave();
    }

    @Test(timeout=1000)
    public void put_retry_1peer() throws Exception {
        Peer peer1 = new Peer(8, 0, InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peer1.join());

        UUID id = new UUID("1234567890-0-1");
        byte[] data = "my data".getBytes();

        DataStorage dataStorage1 = peer1.getDataStorage();
        LocalDataStorage localDataStorage1 = dataStorage1.getLocalDataStorage();

        assertTrue(dataStorage1.put(id, data));
        assertArrayEquals(dataStorage1.get(id), data);

        assertTrue(dataStorage1.put(id, data));

        assertEquals(7, localDataStorage1.getMemoryUsed());
        assertTrue(localDataStorage1.canPut(7));
        assertEquals(new HashSet<UUID>(){{ add(id); }}, localDataStorage1.getAll());
        assertTrue(localDataStorage1.has(id));
        assertArrayEquals(data, localDataStorage1.get(id));

        assertEquals(new HashSet<>(), dataStorage1.getRedirects());
        assertEquals(new HashSet<UUID>(){{ add(id); }}, dataStorage1.getAll());
        assertTrue(dataStorage1.has(id));
        assertArrayEquals(data, dataStorage1.get(id));
        assertFalse(dataStorage1.successorHasStored(id));

        peer1.leave();
    }

    @Test(timeout=1000)
    public void put_get_2peer() throws Exception {
        Peer peer1 = new Peer(8, 0, InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peer1.join());

        Peer peer2 = new Peer(8, 10, InetAddress.getByName("localhost"), Paths.get("bin"));
        peer2.join(peer1.getSocketAddress());

        UUID id = new UUID("1234567890-0-1");
        byte[] data = "my data".getBytes();

        DataStorage dataStorage1 = peer1.getDataStorage();
        LocalDataStorage localDataStorage1 = dataStorage1.getLocalDataStorage();

        DataStorage dataStorage2 = peer2.getDataStorage();
        LocalDataStorage localDataStorage2 = dataStorage2.getLocalDataStorage();

        localDataStorage1.setCapacity(0);

        assertTrue(dataStorage1.put(id, data));

        assertEquals(0, localDataStorage1.getMemoryUsed());
        assertFalse(localDataStorage1.canPut(7));
        assertEquals(new HashSet<UUID>(), localDataStorage1.getAll());
        assertFalse(localDataStorage1.has(id));
        assertNull(localDataStorage1.get(id));

        assertEquals(new HashSet<>(){{ add(id); }}, dataStorage1.getRedirects());
        assertEquals(new HashSet<UUID>(){{ add(id); }}, dataStorage1.getAll());
        assertTrue(dataStorage1.has(id));
        assertArrayEquals(data, dataStorage1.get(id));
        assertTrue(dataStorage1.successorHasStored(id));

        assertEquals(7, localDataStorage2.getMemoryUsed());
        assertTrue(localDataStorage2.canPut(7));
        assertEquals(new HashSet<UUID>(){{ add(id); }}, localDataStorage2.getAll());
        assertTrue(localDataStorage2.has(id));
        assertArrayEquals(data, localDataStorage2.get(id));

        assertEquals(new HashSet<>(), dataStorage2.getRedirects());
        assertEquals(new HashSet<UUID>(), dataStorage2.getAll());
        assertFalse(dataStorage2.has(id));
        assertNull(dataStorage2.get(id));
        assertFalse(dataStorage2.successorHasStored(id));

        peer1.leave();
        peer2.leave();
    }

    @Test(timeout=1000)
    public void delete_2peer() throws Exception {
        Peer peer1 = new Peer(8, 0, InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peer1.join());

        Peer peer2 = new Peer(8, 10, InetAddress.getByName("localhost"), Paths.get("bin"));
        peer2.join(peer1.getSocketAddress());

        UUID id = new UUID("1234567890-0-1");
        byte[] data = "my data".getBytes();

        DataStorage dataStorage1 = peer1.getDataStorage();
        LocalDataStorage localDataStorage1 = dataStorage1.getLocalDataStorage();

        DataStorage dataStorage2 = peer2.getDataStorage();
        LocalDataStorage localDataStorage2 = dataStorage2.getLocalDataStorage();

        localDataStorage1.setCapacity(0);

        assertTrue(dataStorage1.put(id, data));

        assertTrue(dataStorage1.delete(id));

        assertEquals(0, localDataStorage1.getMemoryUsed());
        assertFalse(localDataStorage1.canPut(7));
        assertEquals(new HashSet<UUID>(), localDataStorage1.getAll());
        assertFalse(localDataStorage1.has(id));
        assertNull(localDataStorage1.get(id));

        assertEquals(new HashSet<>(), dataStorage1.getRedirects());
        assertEquals(new HashSet<UUID>(), dataStorage1.getAll());
        assertFalse(dataStorage1.has(id));
        assertNull(dataStorage1.get(id));
        assertFalse(dataStorage1.successorHasStored(id));

        assertEquals(0, localDataStorage2.getMemoryUsed());
        assertTrue(localDataStorage2.canPut(7));
        assertEquals(new HashSet<UUID>(), localDataStorage2.getAll());
        assertFalse(localDataStorage2.has(id));
        assertNull(localDataStorage2.get(id));

        assertEquals(new HashSet<>(), dataStorage2.getRedirects());
        assertEquals(new HashSet<UUID>(), dataStorage2.getAll());
        assertFalse(dataStorage2.has(id));
        assertNull(dataStorage2.get(id));
        assertFalse(dataStorage2.successorHasStored(id));

        peer1.leave();
        peer2.leave();
    }

    @Test(timeout=2000)
    public void redirects_2peer() throws Exception {
        Peer peer1 = new Peer(8, 0, InetAddress.getByName("localhost"), Paths.get("bin"));
        assertTrue(peer1.join());

        Peer peer2 = new Peer(8, 10, InetAddress.getByName("localhost"), Paths.get("bin"));
        peer2.join(peer1.getSocketAddress());

        UUID id1 = new UUID("1234567890-0-1");
        byte[] data1 = "my data".getBytes();
        UUID id2 = new UUID("0987654321-0-1");
        byte[] data2 = "his data".getBytes();

        Chord chord1 = peer1.getChord();
        DataStorage dataStorage1 = peer1.getDataStorage();
        LocalDataStorage localDataStorage1 = dataStorage1.getLocalDataStorage();

        Chord chord2 = peer2.getChord();
        DataStorage dataStorage2 = peer2.getDataStorage();

        localDataStorage1.setCapacity(0);

        assertTrue(dataStorage1.put(id1, data1));
        assertTrue(dataStorage1.put(id2, data2));

        assertEquals(dataStorage2.getRedirects(), new GetRedirectsProtocol(dataStorage1, chord1).invoke());
        assertEquals(dataStorage1.getRedirects(), new GetRedirectsProtocol(dataStorage1, chord1.getNodeInfo().address).invoke());
        assertEquals(dataStorage2.getRedirects(), new GetRedirectsProtocol(dataStorage1, chord2.getNodeInfo().address).invoke());

        assertEquals(dataStorage1.getRedirects(), new GetRedirectsProtocol(dataStorage2, chord2).invoke());
        assertEquals(dataStorage1.getRedirects(), new GetRedirectsProtocol(dataStorage2, chord1.getNodeInfo().address).invoke());
        assertEquals(dataStorage2.getRedirects(), new GetRedirectsProtocol(dataStorage2, chord2.getNodeInfo().address).invoke());

        peer1.leave();
        peer2.leave();
    }
}
