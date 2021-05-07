package sdis.Modules.DataStorage;

import sdis.UUID;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public abstract class DataStorageAbstract {
    abstract public CompletableFuture<Boolean> put(UUID id, byte[] data) throws IOException;
    abstract public CompletableFuture<byte[]> get(UUID id) throws IOException;
    abstract public CompletableFuture<Boolean> delete(UUID id);
}
