package sdis.Modules.DataStorage;

import sdis.UUID;
import sdis.Utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.*;

/**
 * Manage peer chunk storage.
 *
 * There should be at most one instance of this class.
 */
public class LocalDataStorage extends DataStorageAbstract {
    private static final int BUFFER_SIZE = 80000;

    private final Path storagePath;
    private final Executor executor;
    private int capacity;

    public LocalDataStorage(Path storagePath, Executor executor, int capacity){
        this.storagePath = storagePath;
        this.executor = executor;
        this.capacity = capacity;
        createStorage();
    }

    /**
     * @brief Creates a storage directory.
     **/
    private void createStorage(){
        File file = storagePath.toFile();

        // Delete existing folder
        Utils.deleteRecursive(file);

        // Create new folder
        if (file.mkdirs()) {
            System.out.println("Storage created");
        } else {
            System.out.println("Failed to create storage");
        }
    }

    public Path getPath() {
        return storagePath;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity){
        this.capacity = capacity;
    }

    /**
     * @brief Get occupied space in bytes.
     *
     * @return int representing the number of bytes stored.
     **/
    public CompletableFuture<Long> getMemoryUsed(){
        File storage = storagePath.toFile();
        long size = 0;
        for (File file : Objects.requireNonNull(storage.listFiles()))
            size += file.length();
        return CompletableFuture.completedFuture(size);
    }

    public CompletableFuture<Boolean> canPut(int length){
        return getMemoryUsed().thenApplyAsync(memoryUsed -> memoryUsed + length <= getCapacity());
    }

    @Override
    public Set<UUID> getAll(){
        File storage = storagePath.toFile();
        String[] list = Objects.requireNonNull(storage.list());
        Set<UUID> ret = new HashSet<>();
        for(String id: list){
            ret.add(new UUID(id));
        }
        return ret;
    }

    public Boolean has(UUID id){
        File file = new File(storagePath + "/" + id);
        return file.canRead();
    }

    /**
     * @brief Saves a data piece in the backup directory.
     *
     * @param id data piece identifier.
     * @param data Byte array to be stored.
     * @return true if successful, false otherwise.
     **/
    @Override
    public CompletableFuture<Boolean> put(UUID id, byte[] data) {
        return canPut(data.length).thenApplyAsync(canPut -> {
            if(!canPut) return false;
            ByteBuffer buffer = ByteBuffer.wrap(data);
            try {
                AsynchronousFileChannel os = AsynchronousFileChannel.open(Path.of(storagePath + "/" + id), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                os.write(buffer, 0).get();
                os.close();
            } catch (InterruptedException | ExecutionException | IOException e) {
                return false;
            }
            return true;
        });
    }

    @Override
    public CompletableFuture<byte[]> get(UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            if (!has(id)) return null;

            AsynchronousFileChannel is;
            try {
                is = AsynchronousFileChannel.open(Path.of(storagePath + "/" + id), StandardOpenOption.READ);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
            Future<Integer> f = is.read(buffer, 0);

            try {
                int size = f.get();
                is.close();
                byte[] bufferArray = new byte[size];
                buffer.flip();
                buffer.get(bufferArray, 0, size);
                return bufferArray;
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    /**
     * @brief Deletes a data piece in the backup directory.
     *
     * @param id    data piece identifier
     */
    @Override
    public CompletableFuture<Boolean> delete(UUID id){
        File datapiece = new File(storagePath + "/" + id);
        boolean ret = datapiece.delete();
        return CompletableFuture.completedFuture(ret);
    }

    public long getSize(UUID id) {
        File file = new File(storagePath + "/" + id);
        return file.length();
    }
}