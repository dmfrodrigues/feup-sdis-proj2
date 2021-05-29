package sdis.Modules.Main;

import sdis.Modules.Chord.Chord;
import sdis.Modules.Main.Messages.EnlistFileMessage;
import sdis.Modules.ProtocolTask;
import sdis.Modules.SystemStorage.SystemStorage;
import sdis.Storage.ByteArrayChunkIterator;
import sdis.Storage.ChunkIterator;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletionException;

import static sdis.Modules.Main.Main.CHUNK_SIZE;

public class BackupFileProtocol extends MainProtocolTask<Boolean> {
    private final Main main;
    private final Main.File file;
    private final ChunkIterator chunkIterator;
    private final boolean enlist;

    public BackupFileProtocol(Main main, Main.File file, ChunkIterator chunkIterator) {
        this(main, file, chunkIterator, true);
    }

    public BackupFileProtocol(Main main, Main.File file, ChunkIterator chunkIterator, boolean enlist) {
        this.main = main;
        this.file = file;
        this.chunkIterator = chunkIterator;
        this.enlist = enlist;
    }

    public BackupFileProtocol(Main main, Main.File file, byte[] data, boolean enlist) {
        this(main, file, new ByteArrayChunkIterator(data, CHUNK_SIZE), enlist);
    }

    public boolean enlistFile() {
        SystemStorage systemStorage = main.getSystemStorage();
        Chord chord = systemStorage.getChord();
        Chord.NodeInfo s = chord.findSuccessor(file.getOwner().asFile().getChunk(0).getReplica(0).getUUID().getKey(chord));
        try {
            EnlistFileMessage m = new EnlistFileMessage(file);
            Socket socket = main.send(s.address, m);
            byte[] response = readAllBytesAndClose(socket);
            return m.parseResponse(response);
        } catch (IOException | InterruptedException e) {
            throw new CompletionException(e);
        }
    }

    private boolean putReplica(Main.Replica replica, byte[] data) {
        SystemStorage systemStorage = main.getSystemStorage();
        return systemStorage.put(replica.getUUID(), data);
    }

    private boolean putChunk(Main.Chunk chunk, byte[] data){
        List<ProtocolTask<Boolean>> tasks = new ArrayList<>();
        for(int i = 0; i < file.getReplicationDegree(); ++i){
            Main.Replica replica = chunk.getReplica(i);
            tasks.add(new ProtocolTask<>() {
                @Override
                protected Boolean compute() {
                    return putReplica(replica, data);
                }
            });
        }

        return invokeAndReduceTasks(tasks);
    }

    @Override
    public Boolean compute() {
        long numChunks;
        try {
            numChunks = chunkIterator.length();
        } catch (IOException e) {
            return false;
        }

        if(enlist) {
            if (!enlistFile()) return false;
        }

        List<ProtocolTask<Boolean>> tasks = new LinkedList<>();
        for (long i = 0; i < numChunks; ++i) {
            long finalI = i;
            tasks.add(new ProtocolTask<>() {
                @Override
                protected Boolean compute() {
                    byte[] data = chunkIterator.next();
                    return putChunk(file.getChunk(finalI), data);
                }
            });
        }

        return invokeAndReduceTasks(tasks);
    }
}
