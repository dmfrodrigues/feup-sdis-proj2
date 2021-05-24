package sdis.Modules.Main;

import sdis.Modules.Chord.Chord;
import sdis.Modules.Main.Messages.DelistFileMessage;
import sdis.Modules.ProtocolSupplier;
import sdis.Modules.SystemStorage.SystemStorage;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public class DeleteFileProtocol extends ProtocolSupplier<Boolean> {
    private final Main main;
    private final Main.File file;
    private final int maxNumberFutures;
    private final boolean delist;

    public DeleteFileProtocol(Main main, Main.File file, int maxNumberFutures) {
        this(main, file, maxNumberFutures, true);
    }

    public DeleteFileProtocol(Main main, Main.File file, int maxNumberFutures, boolean delist) {
        this.main = main;
        this.file = file;
        this.maxNumberFutures = maxNumberFutures;
        this.delist = delist;
    }

    private CompletableFuture<Boolean> delistFile() {
        SystemStorage systemStorage = main.getSystemStorage();
        Chord chord = systemStorage.getChord();
        return chord.getSuccessor(file.getOwner().asFile().getChunk(0).getReplica(0).getUUID().getKey(chord))
        .thenApplyAsync((Chord.NodeInfo s) -> {
            try {
                DelistFileMessage m = new DelistFileMessage(file);
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

    public CompletableFuture<Boolean> deleteReplica(Main.Replica replica) {
        SystemStorage systemStorage = main.getSystemStorage();
        return systemStorage.delete(replica.getUUID());
    }

    public CompletableFuture<Boolean> deleteChunk(Main.Chunk chunk) {
        List<CompletableFuture<Boolean>> futuresList = new ArrayList<>();
        for(int i = 0; i < file.getReplicationDegree(); ++i){
            Main.Replica replica = chunk.getReplica(i);
            futuresList.add(deleteReplica(replica));
        }

        return CompletableFuture.supplyAsync(() -> {
            boolean success = true;
            for (CompletableFuture<Boolean> f : futuresList) {
                try {
                    success &= f.get();
                } catch (InterruptedException | ExecutionException e) {
                    success = false;
                }
            }
            return success;
        });
    }

    @Override
    public Boolean get() {
        long numChunks = file.getNumberOfChunks();

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

            CompletableFuture<Boolean> f = deleteChunk(file.getChunk(i));

            futuresList.add(f);
        }

        while (!futuresList.isEmpty()) {
            boolean b;
            try {
                b = waitForAny(futuresList);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                b = false;
            }
            if (!b) {
                waitForAll(futuresList);
                return false;
            }
        }

        if(delist) {
            Boolean delistedFile;
            try {
                delistedFile = delistFile().get();
            } catch (InterruptedException e) {
                throw new CompletionException(e);
            } catch (ExecutionException e) {
                throw new CompletionException(e.getCause());
            }
            return delistedFile;
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
