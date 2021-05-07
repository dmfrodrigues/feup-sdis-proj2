package sdis.Modules.DataStorage;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.Messages.DataStorageMessage;
import sdis.UUID;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class DataStorage extends DataStorageAbstract {
    /**
     * Initially reserved storage for backing up chunks (in bytes).
     */
    private static final int INITIAL_STORAGE_SIZE = 1000000000;

    private final LocalDataStorage localDataStorage;
    private final Set<UUID> storedBySuccessor;
    private final Executor executor;
    private final Chord chord;

    public DataStorage(String storagePath, Executor executor, Chord chord){
        localDataStorage = new LocalDataStorage(storagePath, INITIAL_STORAGE_SIZE);
        storedBySuccessor = new HashSet<>();
        this.executor = executor;
        this.chord = chord;
    }

    @Override
    public CompletableFuture<Boolean> put(UUID id, byte[] data) {
        return CompletableFuture.supplyAsync(new PutProtocol(chord, this, id, data), executor);
    }

    @Override
    public CompletableFuture<byte[]> get(UUID id) {
        return CompletableFuture.supplyAsync(new GetProtocol(chord, this, id), executor);
    }

    @Override
    public CompletableFuture<Boolean> delete(UUID id) {
        return CompletableFuture.supplyAsync(new DeleteProtocol(chord, this, id), executor);
    }

    public LocalDataStorage getLocalDataStorage(){
        return localDataStorage;
    }

    public void registerSuccessorStored(UUID id) {
        storedBySuccessor.add(id);
    }

    public void unregisterSuccessorStored(UUID id) {
        storedBySuccessor.remove(id);
    }

    public boolean successorHasStored(UUID id) {
        return storedBySuccessor.contains(id);
    }

    public Socket send(InetSocketAddress to, DataStorageMessage m) throws IOException {
        Socket socket = new Socket(to.getAddress(), to.getPort());
        OutputStream os = socket.getOutputStream();
        os.write(m.asByteArray());
        os.flush();
        return socket;
    }
}
