package sdis.Modules.Main;

import sdis.Modules.Chord.Chord;
import sdis.Modules.Main.Messages.EnlistFileMessage;
import sdis.Modules.ProtocolSupplier;
import sdis.Modules.SystemStorage.SystemStorage;
import sdis.Storage.ByteArrayChunkIterator;
import sdis.Storage.ChunkIterator;
import sdis.UUID;

import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import static sdis.Modules.Main.Main.CHUNK_SIZE;

public class BackupFileProtocol extends ProtocolSupplier<Boolean> {
    private final Main main;
    private final Main.File file;
    private final ChunkIterator chunkIterator;
    private final int maxNumberFutures;

    public BackupFileProtocol(Main main, Main.File file, byte[] data, int maxNumberFutures) throws IOException {
        this.main = main;
        this.file = file;
        this.chunkIterator = new ByteArrayChunkIterator(data, CHUNK_SIZE);
        this.maxNumberFutures = maxNumberFutures;

    }

    private CompletableFuture<Boolean> enlistFile() {
        SystemStorage systemStorage = main.getSystemStorage();
        Chord chord = systemStorage.getChord();
        return chord.getSuccessor(file.getOwner().toUUID().getKey(chord))
        .thenApplyAsync((Chord.NodeInfo s) -> {
            try {
                EnlistFileMessage m = new EnlistFileMessage(file);
                Socket socket = main.send(s.address, m);
                socket.shutdownOutput();
                byte[] response = socket.getInputStream().readAllBytes();
                socket.close();
                return m.parseResponse(response);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
    }

    private CompletableFuture<Boolean> putChunk(long chunkIndex, byte[] data) {
        SystemStorage systemStorage = main.getSystemStorage();

        CompletableFuture<Boolean>[] futuresList = new CompletableFuture[file.getReplicationDegree()];
        for(int i = 0; i < file.getReplicationDegree(); ++i){
            UUID uuid = file.getChunk(chunkIndex).getReplica(i).getUUID();
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

        Boolean enlistedFile;
        try {
            enlistedFile = enlistFile().get();
        } catch (InterruptedException e) {
            throw new CompletionException(e);
        } catch (ExecutionException e) {
            throw new CompletionException(e.getCause());
        }
        if(!enlistedFile) return false;

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
