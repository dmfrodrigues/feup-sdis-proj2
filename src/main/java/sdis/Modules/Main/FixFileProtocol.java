package sdis.Modules.Main;

import sdis.Modules.ProtocolTask;
import sdis.Modules.SystemStorage.SystemStorage;
import sdis.UUID;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class FixFileProtocol extends MainProtocolTask<Boolean> {
    private final Main main;
    private final Main.File file;

    public FixFileProtocol(Main main, Main.File file){
        this.main = main;
        this.file = file;
    }

    protected boolean fixChunk(long chunkIndex) {
        SystemStorage systemStorage = main.getSystemStorage();
        int replicationDegree = file.getReplicationDegree();

        List<ProtocolTask<Boolean>> tasks = new ArrayList<>();
        for(int i = 0; i < replicationDegree; ++i){
            UUID id = file.getChunk(chunkIndex).getReplica(i).getUUID();
            tasks.add(new ProtocolTask<>() {
                @Override
                protected Boolean compute() {
                    return systemStorage.head(id);
                }
            });
        }

        ProtocolTask.invokeTasks(tasks.stream().map((ProtocolTask<Boolean> task) -> (ProtocolTask<?>) task).collect(Collectors.toList()));

        try {
            byte[] chunk = null;
            for (int i = 0; i < replicationDegree; ++i) {
                UUID id = file.getChunk(chunkIndex).getReplica(i).getUUID();
                ProtocolTask<Boolean> task = tasks.get(i);
                if (task.get()) {
                    chunk = systemStorage.get(id);
                    break;
                }
            }
            if(chunk == null) return false;

            boolean ret = true;
            for (int i = 0; i < replicationDegree; ++i) {
                boolean b = tasks.get(i).get();
                if (!b) {
                    UUID id = file.getChunk(chunkIndex).getReplica(i).getUUID();
                    ret &= systemStorage.put(id, chunk);
                }
            }
            return ret;
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Boolean compute() {
        List<ProtocolTask<Boolean>> tasks = new LinkedList<>();
        for (long i = 0; i < file.getNumberOfChunks(); ++i) {
            long finalI = i;
            ProtocolTask<Boolean> task = new ProtocolTask<>() {
                @Override
                protected Boolean compute() {
                    return fixChunk(finalI);
                }
            };
            tasks.add(task);
        }
        return invokeAndReduceTasks(tasks);
    }
}
