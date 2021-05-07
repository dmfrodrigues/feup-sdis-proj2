package sdis.Protocols.DataStorage;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class DatapieceStorageManager {
    abstract public List<String> getDatapieces();
    abstract public boolean saveDatapiece(String id, byte[] data) throws IOException;
    abstract public boolean hasDatapiece(String id);
    abstract public CompletableFuture<byte[]> getDatapiece(String id) throws IOException;
    abstract public void deleteDatapiece(String id);
}
