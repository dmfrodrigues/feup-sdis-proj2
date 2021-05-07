package sdis.Protocols.DataStorage;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GlobalDatapieceStorageManager extends DatapieceStorageManager {

    LocalDatapieceStorageManager localManager;

    public GlobalDatapieceStorageManager(LocalDatapieceStorageManager localManager){
        this.localManager = localManager;
    }

    @Override
    public List<String> getDatapieces() {
        return null;
    }

    @Override
    public boolean saveDatapiece(String id, byte[] data) throws IOException {
        return false;
    }

    @Override
    public boolean hasDatapiece(String id) {
        return false;
    }

    @Override
    public CompletableFuture<byte[]> getDatapiece(String id) throws IOException {
        return null;
    }

    @Override
    public void deleteDatapiece(String id) {

    }
}
