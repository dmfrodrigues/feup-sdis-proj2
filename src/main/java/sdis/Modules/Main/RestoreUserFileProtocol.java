package sdis.Modules.Main;

import sdis.Modules.ProtocolSupplier;
import sdis.Modules.SystemStorage.SystemStorage;
import sdis.Storage.ChunkOutput;
import sdis.UUID;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public class RestoreUserFileProtocol extends ProtocolSupplier<Boolean> {
    private final Main main;
    private final String id;
    private final int replicationDegree;
    private ChunkOutput destination;
    private int maxNumberFutures;

    public RestoreUserFileProtocol(Main main, String id, int replicationDegree, ChunkOutput destination, int maxNumberFutures){
        this.main = main;
        this.id = id;
        this.replicationDegree = replicationDegree;
        this.destination = destination;
        this.maxNumberFutures = maxNumberFutures;
    }

    private CompletableFuture<byte[]> getChunk(long chunkIndex) {
        SystemStorage systemStorage = main.getSystemStorage();

        CompletableFuture<byte[]>[] futuresList = new CompletableFuture[replicationDegree];
        for(int i = 0; i < replicationDegree; ++i){
            UUID uuid = new UUID(id + "-" + chunkIndex + "-" + i);
            futuresList[i] = systemStorage.get(uuid);
        }

        CompletableFuture<byte[]> allOfFuture = CompletableFuture.allOf(futuresList)
            .thenApply((ignored) -> {
                try {
                    byte[] ret = null;
                    for (CompletableFuture<byte[]> f : futuresList) {
                        if (f.get() != null) {
                            ret = f.get();
                            break;
                        }
                    }
                    if(ret == null) return null;
                    for (int i = 0; i < replicationDegree; ++i) {
                        byte[] tmp = futuresList[i].get();
                        if (tmp == null) {
                            UUID uuid = new UUID(id + "-" + chunkIndex + "-" + i);
                            systemStorage.put(uuid, ret);
                        }
                    }
                    return ret;
                } catch (InterruptedException e) {
                    throw new CompletionException(e);
                } catch (ExecutionException e) {
                    throw new CompletionException(e.getCause());
                }
            });

        return allOfFuture;
    }

    @Override
    public Boolean get() {
        for (long i = 0; ; ++i) {
            long finalI = i;
            CompletableFuture<Boolean> f = getChunk(i)
                .thenApplyAsync((byte[] data) -> {
                    if(data == null) return false;
                    destination.set(finalI, data);
                    return true;
                });

            try {
                if(!f.get())
                    return true;
            } catch (InterruptedException | ExecutionException e) {
                return false;
            }
        }
    }
}
