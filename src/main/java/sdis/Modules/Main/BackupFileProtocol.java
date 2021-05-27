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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RecursiveTask;

import static sdis.Modules.Main.Main.CHUNK_SIZE;

public class BackupFileProtocol extends ProtocolTask<Boolean> {
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

    public BackupFileProtocol(Main main, Main.File file, byte[] data) {
        this(main, file, data, true);
    }

    public BackupFileProtocol(Main main, Main.File file, byte[] data, boolean enlist) {
        this(main, file, new ByteArrayChunkIterator(data, CHUNK_SIZE), enlist);
    }

    public boolean enlistFile() {
        SystemStorage systemStorage = main.getSystemStorage();
        Chord chord = systemStorage.getChord();
        Chord.NodeInfo s = chord.getSuccessor(file.getOwner().asFile().getChunk(0).getReplica(0).getUUID().getKey(chord));
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
    }

    private boolean putReplica(Main.Replica replica, byte[] data) {
        SystemStorage systemStorage = main.getSystemStorage();
        return systemStorage.put(replica.getUUID(), data);
    }

    private boolean putChunk(Main.Chunk chunk, byte[] data){
        List<RecursiveTask<Boolean>> tasks = new ArrayList<>();
        for(int i = 0; i < file.getReplicationDegree(); ++i){
            Main.Replica replica = chunk.getReplica(i);
            tasks.add(new RecursiveTask<>() {
                @Override
                protected Boolean compute() {
                    return putReplica(replica, data);
                }
            });
        }

        invokeAll(tasks);
        return tasks.stream().map((RecursiveTask<Boolean> task) -> {
            try {
                return task.get();
            } catch (InterruptedException | ExecutionException e) {
                return false;
            }
        }).reduce((Boolean a, Boolean b) -> a && b).orElse(true);
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

        List<RecursiveTask<Boolean>> tasks = new LinkedList<>();
        for (long i = 0; i < numChunks; ++i) {
            try {
                byte[] data = chunkIterator.next().get();
                long finalI = i;
                tasks.add(new ProtocolTask<>() {
                    @Override
                    protected Boolean compute() {
                        return putChunk(file.getChunk(finalI), data);
                    }
                });
            } catch (InterruptedException | ExecutionException e) {
                return false;
            }
        }

        invokeAll(tasks);
        return tasks.stream().map((RecursiveTask<Boolean> task) -> {
            try {
                return task.get();
            } catch (InterruptedException | ExecutionException e) {
                return false;
            }
        }).reduce((Boolean a, Boolean b) -> a && b).orElse(true);
    }
}
