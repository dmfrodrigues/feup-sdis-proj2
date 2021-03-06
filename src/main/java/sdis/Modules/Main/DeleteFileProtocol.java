package sdis.Modules.Main;

import sdis.Modules.Chord.Chord;
import sdis.Modules.Main.Messages.DelistFileMessage;
import sdis.Modules.ProtocolTask;
import sdis.Modules.SystemStorage.SystemStorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletionException;

public class DeleteFileProtocol extends MainProtocolTask<Boolean> {
    private final Main main;
    private final Main.File file;
    private final boolean delist;

    public DeleteFileProtocol(Main main, Main.File file) {
        this(main, file, true);
    }

    public DeleteFileProtocol(Main main, Main.File file, boolean delist) {
        this.main = main;
        this.file = file;
        this.delist = delist;
    }

    private boolean delistFile() {
        SystemStorage systemStorage = main.getSystemStorage();
        Chord chord = systemStorage.getChord();
        Chord.NodeInfo s = chord.findSuccessor(file.getOwner().asFile().getChunk(0).getReplica(0).getUUID().getKey(chord));
        try {
            DelistFileMessage m = new DelistFileMessage(file);
            return m.sendTo(s.address);
        } catch (IOException | InterruptedException e) {
            throw new CompletionException(e);
        }
    }

    public Boolean deleteReplica(Main.Replica replica) {
        SystemStorage systemStorage = main.getSystemStorage();
        return systemStorage.delete(replica.getUUID());
    }

    public Boolean deleteChunk(Main.Chunk chunk) {
        List<ProtocolTask<Boolean>> tasks = new ArrayList<>();
        for(int i = 0; i < file.getReplicationDegree(); ++i){
            Main.Replica replica = chunk.getReplica(i);
            tasks.add(new ProtocolTask<>() {
                @Override
                protected Boolean compute() {
                    return deleteReplica(replica);
                }
            });
        }

        return invokeAndReduceTasks(tasks);
    }

    @Override
    public Boolean compute() {
        long numChunks = file.getNumberOfChunks();

        List<ProtocolTask<Boolean>> tasks = new LinkedList<>();
        for (long i = 0; i < numChunks; ++i) {
            long finalI = i;
            tasks.add(new ProtocolTask<>() {
                @Override
                protected Boolean compute() {
                    return deleteChunk(file.getChunk(finalI));
                }
            });
        }

        if(!invokeAndReduceTasks(tasks)){
            return false;
        }

        if(delist) {
            return delistFile();
        }

        return true;
    }
}
