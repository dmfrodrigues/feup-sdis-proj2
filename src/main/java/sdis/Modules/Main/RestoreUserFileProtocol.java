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
    private final Username username;
    private final int replicationDegree;
    private final ChunkOutput destination;

    public RestoreUserFileProtocol(Main main, Username username, int replicationDegree, ChunkOutput destination){
        this.main = main;
        this.username = username;
        this.replicationDegree = replicationDegree;
        this.destination = destination;
    }

    private CompletableFuture<byte[]> getChunk(long chunkIndex) {
        SystemStorage systemStorage = main.getSystemStorage();

        CompletableFuture<byte[]>[] futuresList = new CompletableFuture[replicationDegree];
        for(int i = 0; i < replicationDegree; ++i){
            UUID uuid = new UUID(username.getPath().toString() + "-" + chunkIndex + "-" + i);
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
                            UUID uuid = new UUID(username.getPath().toString() + "-" + chunkIndex + "-" + i);
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
        long i;
        for (i = 0; ; ++i) {
            long finalI = i;
            CompletableFuture<Boolean> f = getChunk(i)
                .thenApplyAsync((byte[] data) -> {
                    if(data == null) return false;
                    destination.set(finalI, data);
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
