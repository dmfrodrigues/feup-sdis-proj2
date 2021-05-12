package sdis.Modules.Main;

import sdis.Modules.ProtocolSupplier;
import sdis.Modules.SystemStorage.SystemStorage;
import sdis.Storage.ByteArrayChunkIterator;
import sdis.Storage.ChunkIterator;
import sdis.UUID;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import static sdis.Modules.Main.Main.CHUNK_SIZE;

public class BackupFileProtocol extends ProtocolSupplier<Boolean> {
    private final Main main;
    private final int replicationDegree;
    private final ChunkIterator chunkIterator;
    private int maxNumberFutures;

    public BackupFileProtocol(Main main, UserMetadata.File file, byte[] data, int maxNumberFutures) throws IOException {
        this(main, "u/" + file.getPath(), file.getReplicationDegree(), data, maxNumberFutures);
    }

    public BackupFileProtocol(Main main, String id, int replicationDegree, byte[] data, int maxNumberFutures) throws IOException {
        this.main = main;
        this.replicationDegree = replicationDegree;
        this.chunkIterator = new ByteArrayChunkIterator(id, data, CHUNK_SIZE);
        this.maxNumberFutures = maxNumberFutures;
    }

    private CompletableFuture<Boolean> putChunk(long chunkIndex, byte[] data) {
        SystemStorage systemStorage = main.getSystemStorage();

        CompletableFuture<Boolean>[] futuresList = new CompletableFuture[replicationDegree];
        for(int i = 0; i < replicationDegree; ++i){
            UUID uuid = new UUID(chunkIterator.getFileId() + "-" + chunkIndex + "-" + i);
            futuresList[i] = systemStorage.put(uuid, data);
        }

        return CompletableFuture.allOf(futuresList)
            .thenApply((ignored) -> {
                try {
                    boolean success = true;
                    for (CompletableFuture<Boolean> f : futuresList) {
                        success &= f.get();
                    }
                    return success;
                } catch (InterruptedException | ExecutionException e) {
                    return false;
                }
            });
    }

    @Override
    public Boolean get() {
        long numChunks;
        try {
            numChunks = chunkIterator.length();
        } catch (IOException e) {
            return false;
        }

        List<CompletableFuture<Boolean>> futuresList = new LinkedList<>();
        for (long i = 0; i < numChunks; ++i) {
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

            byte[] data;
            try {
                data = chunkIterator.next().get();
            } catch (InterruptedException | ExecutionException e) {
                return false;
            }
            CompletableFuture<Boolean> f = putChunk(i, data);

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
