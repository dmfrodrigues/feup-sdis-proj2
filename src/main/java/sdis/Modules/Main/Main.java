package sdis.Modules.Main;

import sdis.Modules.Main.Messages.MainMessage;
import sdis.Modules.SystemStorage.SystemStorage;
import sdis.Storage.ChunkIterator;
import sdis.Storage.ChunkOutput;
import sdis.UUID;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class Main {
    public static final int CHUNK_SIZE = 64000;
    public static final int USER_METADATA_REPDEG = 10;

    private final SystemStorage systemStorage;
    private Executor executor;

    public Main(SystemStorage systemStorage, Executor executor){
        this.systemStorage = systemStorage;
        this.executor = executor;
    }

    public SystemStorage getSystemStorage(){
        return systemStorage;
    }

    public Socket send(InetSocketAddress to, MainMessage m) throws IOException {
        Socket socket = new Socket(to.getAddress(), to.getPort());
        OutputStream os = socket.getOutputStream();
        os.write(m.asByteArray());
        os.flush();
        return socket;
    }

    public CompletableFuture<Boolean> backupFile(Main.File file, ChunkIterator chunkIterator) {
        return CompletableFuture.supplyAsync(new BackupFileProtocol(this, file, chunkIterator, 10), executor);
    }

    public CompletableFuture<Boolean> restoreFile(Main.File file, ChunkOutput destination) {
        return CompletableFuture.supplyAsync(new RestoreFileProtocol(this, file, destination, 10), executor);
    }

    public CompletableFuture<Boolean> deleteFile(Main.File file) {
        return CompletableFuture.supplyAsync(new DeleteFileProtocol(this, file, 10), executor);
    }

    public Executor getExecutor() {
        return executor;
    }

    public static class Path implements Serializable {
        String s;

        public Path(String s){
            this.s = s;
        }

        @Override
        public String toString() {
            return s;
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj) return true;
            if(obj == null) return false;
            if(getClass() != obj.getClass()) return false;
            Main.Path path = (Main.Path) obj;
            return s.equals(path.s);
        }

        @Override
        public int hashCode() {
            return s.hashCode();
        }
    }

    public static class File implements Serializable {
        private final Username owner;
        private final Main.Path path;
        private final long numberOfChunks;
        private final int replicationDegree;

        public File(Username owner, Main.Path path, long numberOfChunks, int replicationDegree){
            this.owner = owner;
            this.path = path;
            this.numberOfChunks = numberOfChunks;
            this.replicationDegree = replicationDegree;
        }

        public Username getOwner() {
            return owner;
        }

        public Main.Path getPath() {
            return path;
        }

        public long getNumberOfChunks() {
            return numberOfChunks;
        }

        public int getReplicationDegree() {
            return replicationDegree;
        }

        public Main.Chunk getChunk(long chunkIndex) {
            return new Main.Chunk(this, chunkIndex);
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj) return true;
            if(obj == null) return false;
            if(getClass() != obj.getClass()) return false;
            Main.File file = (Main.File) obj;
            return
                path.equals(file.path) &&
                numberOfChunks == file.numberOfChunks &&
                replicationDegree == file.replicationDegree
            ;
        }

        @Override
        public int hashCode() {
            return path.hashCode();
        }

        @Override
        public String toString() {
            return owner.toString() + "/" + path.toString();
        }
    }

    public static class UserMetadataFile extends File {
        public UserMetadataFile(Username owner, long numberOfChunks) {
            super(owner, null, numberOfChunks, USER_METADATA_REPDEG);
        }

        @Override
        public String toString() {
            return getOwner().toString();
        }
    }

    public static class Chunk {
        private final Main.File file;
        private final long chunkIndex;

        private Chunk(Main.File file, long chunkIndex){
            this.file = file;
            this.chunkIndex = chunkIndex;
        }

        public Replica getReplica(int replicaIndex) {
            return new Main.Replica(this, replicaIndex);
        }

        @Override
        public String toString() {
            return file.toString() + "-" + chunkIndex;
        }

        public Main.File getFile() {
            return file;
        }
    }

    public static class Replica {
        private final Main.Chunk chunk;
        private final int replicaIndex;

        private Replica(Main.Chunk chunk, int replicaIndex){
            this.chunk = chunk;
            this.replicaIndex = replicaIndex;
        }

        @Override
        public String toString() {
            return chunk.toString() + "-" + replicaIndex;
        }

        public UUID getUUID() {
            return new UUID(toString());
        }
    }
}
