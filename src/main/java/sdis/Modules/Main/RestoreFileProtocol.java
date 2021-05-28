package sdis.Modules.Main;

import sdis.Modules.ProtocolTask;
import sdis.Modules.SystemStorage.SystemStorage;
import sdis.Storage.ChunkOutput;
import sdis.UUID;
import sdis.Utils.FixedSizeList;

import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RecursiveTask;

public class RestoreFileProtocol extends MainProtocolTask<Boolean> {
    private final Main main;
    private final Main.File file;
    private final ChunkOutput destination;
    private final int maxNumberFutures;

    public RestoreFileProtocol(Main main, Main.File file, ChunkOutput destination, int maxNumberFutures){
        this.main = main;
        this.file = file;
        this.destination = destination;
        this.maxNumberFutures = maxNumberFutures;
    }

    protected ChunkOutput getDestination(){
        return destination;
    }

    protected byte[] getChunk(long chunkIndex) {
        SystemStorage systemStorage = main.getSystemStorage();
        int replicationDegree = file.getReplicationDegree();

        List<ProtocolTask<byte[]>> tasks = new ArrayList<>();
        for(int i = 0; i < replicationDegree; ++i){
            UUID id = file.getChunk(chunkIndex).getReplica(i).getUUID();
            tasks.add(new ProtocolTask<>() {
                @Override
                protected byte[] compute() {
                    return systemStorage.get(id);
                }
            });
        }

        invokeAll(tasks);
        try {
            byte[] ret = null;
            for (ProtocolTask<byte[]> task : tasks) {
                if (task.get() != null) {
                    ret = task.get();
                    break;
                }
            }
            if(ret == null) return null;
            for (int i = 0; i < replicationDegree; ++i) {
                byte[] tmp = tasks.get(i).get();
                if (tmp == null) {
                    UUID id = file.getChunk(chunkIndex).getReplica(i).getUUID();
                    systemStorage.put(id, ret);
                }
            }
            return ret;
        } catch (InterruptedException e) {
            throw new CompletionException(e);
        } catch (ExecutionException e) {
            throw new CompletionException(e.getCause());
        }
    }

    @Override
    public Boolean compute() {
        Queue<ProtocolTask<Boolean>> tasks = new FixedSizeList<>(maxNumberFutures);
        for (long i = 0; i < file.getNumberOfChunks(); ++i) {
            while (tasks.size() >= maxNumberFutures) {
                ProtocolTask<Boolean> task = tasks.remove();

                boolean b;
                try {
                    b = task.get();
                } catch (ExecutionException | InterruptedException e) {
                    b = false;
                }
                if (!b) {
                    reduceTasks(tasks);
                    return false;
                }
            }

            long finalI = i;
            ProtocolTask<Boolean> task = new ProtocolTask<>() {
                @Override
                protected Boolean compute() {
                    byte[] data = getChunk(finalI);
                    if (data == null) return false;
                    return destination.set(finalI, data);
                }
            };
            task.fork();
            tasks.add(task);
        }

        return reduceTasks(tasks);
    }
}
