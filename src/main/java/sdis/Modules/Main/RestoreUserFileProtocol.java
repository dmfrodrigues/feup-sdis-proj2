package sdis.Modules.Main;

import sdis.Storage.ChunkOutput;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class RestoreUserFileProtocol extends RestoreFileProtocol {
    public RestoreUserFileProtocol(Main main, Username username, ChunkOutput destination, int maxNumberFutures){
        super(main, username.asFile(), destination, maxNumberFutures);
    }

    @Override
    public Boolean get() {
        long i;
        for (i = 0; ; ++i) {
            long finalI = i;
            CompletableFuture<Boolean> f = getChunk(i)
                .thenApplyAsync((byte[] data) -> {
                    if(data == null) return false;
                    getDestination().set(finalI, data);
                    return true;
                });

            try {
                if(!f.get())
                    break;
            } catch (InterruptedException | ExecutionException e) {
                return false;
            }
        }
        return (i != 0);
    }
}
