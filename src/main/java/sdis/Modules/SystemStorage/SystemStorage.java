package sdis.Modules.SystemStorage;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.DataStorage;
import sdis.Modules.SystemStorage.Messages.SystemStorageMessage;
import sdis.UUID;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class SystemStorage {
    private final Chord chord;
    private final DataStorage dataStorage;
    private Executor executor;

    public SystemStorage(Chord chord, DataStorage dataStorage, Executor executor){
        this.chord = chord;
        this.dataStorage = dataStorage;
        this.executor = executor;
    }

    public Chord getChord() {
        return chord;
    }

    public DataStorage getDataStorage() {
        return dataStorage;
    }

    public Socket send(InetSocketAddress to, SystemStorageMessage m) throws IOException {
        Socket socket = new Socket(to.getAddress(), to.getPort());
        OutputStream os = socket.getOutputStream();
        os.write(m.asByteArray());
        os.flush();
        return socket;
    }

    public CompletableFuture<Boolean> put(UUID id, byte[] data) {
        return CompletableFuture.supplyAsync(new PutSystemProtocol(this, id, data), executor);
    }

    public CompletableFuture<byte[]> get(UUID id) {
        return CompletableFuture.supplyAsync(new GetSystemProtocol(this, id), executor);
    }

    public CompletableFuture<Boolean> delete(UUID id) {
        return CompletableFuture.supplyAsync(new DeleteSystemProtocol(this, id), executor);
    }
}
