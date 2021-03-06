package sdis.Modules.Main;

import sdis.Modules.ProtocolTask;
import sdis.Modules.SystemStorage.SystemStorage;
import sdis.Storage.ChunkOutput;
import sdis.UUID;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class RestoreFileProtocol extends MainProtocolTask<Boolean> {
    private final Main main;
    private final Main.File file;
    private final ChunkOutput destination;

    public RestoreFileProtocol(Main main, Main.File file, ChunkOutput destination){
        this.main = main;
        this.file = file;
        this.destination = destination;
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

        ProtocolTask.invokeTasks(tasks.stream().map((ProtocolTask<byte[]> task) -> (ProtocolTask<?>) task).collect(Collectors.toList()));
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
        List<ProtocolTask<Boolean>> finishedTasks = new LinkedList<>();
        Queue<ProtocolTask<Boolean>> tasks = new LinkedList<>();
        for (long i = 0; i < file.getNumberOfChunks(); ++i) {
            while(i > destination.getMaxIndex()){
                ProtocolTask<Boolean> task = tasks.remove();
                task.join();
                finishedTasks.add(task);
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

        return reduceTasks(finishedTasks) && reduceTasks(tasks);
    }
}
