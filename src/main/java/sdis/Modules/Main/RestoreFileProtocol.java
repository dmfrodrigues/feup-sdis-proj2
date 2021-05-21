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

public class RestoreFileProtocol extends ProtocolSupplier<Boolean> {
    private final Main main;
    private final Main.File file;
    private ChunkOutput destination;
    private int maxNumberFutures;

    public RestoreFileProtocol(Main main, Main.File file, ChunkOutput destination, int maxNumberFutures){
        this.main = main;
        this.file = file;
        this.destination = destination;
        this.maxNumberFutures = maxNumberFutures;
    }

    private CompletableFuture<byte[]> getChunk(long chunkIndex) {
        SystemStorage systemStorage = main.getSystemStorage();
        int replicationDegree = file.getReplicationDegree();

        CompletableFuture<byte[]>[] futuresList = new CompletableFuture[replicationDegree];
        for(int i = 0; i < replicationDegree; ++i){
            UUID id = new UUID("f/" + file.getPath() + "-" + chunkIndex + "-" + i);
            futuresList[i] = systemStorage.get(id);
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
                            UUID id = new UUID("f/" + file.getPath() + "-" + chunkIndex + "-" + i);
                            systemStorage.put(id, ret);
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
        List<CompletableFuture<Boolean>> futuresList = new LinkedList<>();
        for (long i = 0; i < file.getNumberOfChunks(); ++i) {
            if (futuresList.size() >= maxNumberFutures) {
                boolean b;
                try {
                    b = waitForAny(futuresList);
                } catch (ExecutionException | InterruptedException e) {
                    b = false;
                }
                if (!b) {
                    waitForAll(futuresList);
                    return false;
                }
            }

            long finalI = i;
            CompletableFuture<Boolean> f = getChunk(i)
                .thenApplyAsync((byte[] data) -> {
                    if(data == null) return false;
                    destination.set(finalI, data);
                    return true;
                });

            futuresList.add(f);
        }

        while (!futuresList.isEmpty()) {
            boolean b;
            try {
                b = waitForAny(futuresList);
            } catch (ExecutionException | InterruptedException e) {
                b = false;
            }
            if (!b) {
                waitForAll(futuresList);
                return false;
            }
        }

        return true;
    }

    private void waitForAll(List<CompletableFuture<Boolean>> futuresList){
        for (CompletableFuture<Boolean> f: futuresList) {
            try {
                f.get();
            } catch (ExecutionException | InterruptedException ignored) {
            }
        }
    }

    private boolean waitForAny(List<CompletableFuture<Boolean>> futuresList) throws ExecutionException, InterruptedException {
        boolean ret = true;

        CompletableFuture<Object> anyOfFuture = CompletableFuture.anyOf(futuresList.toArray(new CompletableFuture[0]));
        anyOfFuture.get();
        ListIterator<CompletableFuture<Boolean>> it = futuresList.listIterator();
        while (it.hasNext()) {
            CompletableFuture<Boolean> f = it.next();
            if (f.isDone()) {
                it.remove();
                ret &= f.get();
            }
        }
        return ret;
    }
}
