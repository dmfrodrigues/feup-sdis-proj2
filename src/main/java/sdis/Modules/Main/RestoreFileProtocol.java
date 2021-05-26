package sdis.Modules.Main;

import sdis.Modules.ProtocolTask;
import sdis.Modules.SystemStorage.SystemStorage;
import sdis.Storage.ChunkOutput;
import sdis.UUID;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RecursiveTask;

public class RestoreFileProtocol extends ProtocolTask<Boolean> {
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
        List<ProtocolTask<Boolean>> tasks = new LinkedList<>();
        for (long i = 0; i < file.getNumberOfChunks(); ++i) {
            if (tasks.size() >= maxNumberFutures) {
                boolean b;
                try {
                    b = waitForAny(tasks);
                } catch (ExecutionException | InterruptedException e) {
                    b = false;
                }
                if (!b) {
                    waitForAll(tasks);
                    return false;
                }
            }

            long finalI = i;
            ProtocolTask<Boolean> task = new ProtocolTask<>() {
                @Override
                protected Boolean compute() {
                    byte[] data = getChunk(finalI);
                    if (data == null) return false;
                    destination.set(finalI, data);
                    return true;
                }
            };
            task.fork();
            tasks.add(task);
        }

        boolean ret = true;
        for(RecursiveTask<Boolean> task: tasks) {
            try {
                ret &= task.get();
            } catch (InterruptedException | ExecutionException e) {
                ret = false;
            }
        }
        return ret;
    }

    private void waitForAll(List<ProtocolTask<Boolean>> futuresList){
        while(!futuresList.isEmpty()) {
            try {
                waitForAny(futuresList);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean waitForAny(List<ProtocolTask<Boolean>> tasks) throws ExecutionException, InterruptedException {
        Iterator<ProtocolTask<Boolean>> iterator = tasks.iterator();
        ProtocolTask<Boolean> task = iterator.next();
        iterator.remove();
        return task.get();
    }
}
