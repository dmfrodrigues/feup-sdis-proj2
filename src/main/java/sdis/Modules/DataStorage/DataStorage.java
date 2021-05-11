package sdis.Modules.DataStorage;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.Messages.DataStorageMessage;
import sdis.UUID;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class DataStorage extends DataStorageAbstract {
    /**
     * Initially reserved storage for backing up chunks (in bytes).
     */
    private static final int INITIAL_STORAGE_SIZE = 1000000000;

    private final LocalDataStorage localDataStorage;
    private final Set<UUID> storedBase = new HashSet<>();
    private final Set<UUID> storedBySuccessor = new HashSet<>();
    private final Executor executor;
    private final Chord chord;

    public DataStorage(Path storagePath, Executor executor, Chord chord){
        localDataStorage = new LocalDataStorage(storagePath, executor, INITIAL_STORAGE_SIZE);
        this.executor = executor;
        this.chord = chord;
    }

    public void storeBase(UUID id) {
        storedBase.add(id);
    }

    public void unstoreBase(UUID id){
        storedBase.remove(id);
    }

    @Override
    public Boolean has(UUID id){
        return storedBase.contains(id);
    }

    @Override
    public Set<UUID> getAll() {
        return storedBase;
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

    public Set<UUID> getRedirects() {
        return storedBySuccessor;
    }
}
