package sdis.Modules.DataStorage;

import sdis.UUID;

import java.util.concurrent.CompletableFuture;

public abstract class DataStorageAbstract {
    abstract public CompletableFuture<Boolean> put(UUID id, byte[] data);
    abstract public CompletableFuture<byte[]> get(UUID id);
    abstract public CompletableFuture<Boolean> delete(UUID id);
}
