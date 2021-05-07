package sdis.Protocols.DataStorage;

import sdis.Peer;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Manage peer chunk storage.
 *
 * There should be at most one instance of this class.
 */
public class LocalDatapieceStorageManager extends DatapieceStorageManager {
    private static final int BUFFER_SIZE = 80000;

    private Peer peer;
    private int capacity;
    private final String storagePath;

    public LocalDatapieceStorageManager(Peer peer, String storagePath, int capacity){
        this.peer = peer;
        this.storagePath = storagePath;
        this.capacity = capacity;
        createStorage();
    }

    /**
     * @brief Creates a storage directory.
     **/
    private void createStorage(){
        File file = new File(storagePath);

        if (file.mkdirs()) {
            System.out.println("Storage created");
        } else {
            System.out.println("Failed to create storage");
        }
    }

    public String getStoragePath() {
        return storagePath;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity){
        this.capacity = capacity;
    }

    /**
     * @brief Get list of IDs of stored datapieces
     *
     * @return List of IDs of stored datapieces
     */
    @Override
    public List<String> getDatapieces(){
        File storage = new File(storagePath);
        return Arrays.asList(Objects.requireNonNull(storage.list()));
    }

    /**
     * @brief Get occupied space in bytes.
     *
     * @return int representing the number of bytes stored.
     **/
    public int getMemoryUsed(){
        File storage = new File(storagePath);
        int size = 0;
        for (File file : Objects.requireNonNull(storage.listFiles()))
            size += file.length();
        return size;
    }

    public boolean canSaveDatapiece(int length){
        return getMemoryUsed() + length > getCapacity();
    }

    /**
     * @brief Saves a data piece in the backup directory.
     *
     * @param id data piece identifier.
     * @param data Byte array to be stored.
     * @return true if successful, false otherwise.
     **/
    @Override
    public boolean saveDatapiece(String id, byte[] data) throws IOException {
        if(!canSaveDatapiece(data.length)) return false;
        ByteBuffer buffer = ByteBuffer.wrap(data);
        AsynchronousFileChannel os = AsynchronousFileChannel.open(Path.of(storagePath + "/" + id), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        try {
            os.write(buffer, 0).get();
        } catch (InterruptedException | ExecutionException e) {
            return false;
        }
        os.close();
        return true;
    }

    @Override
    public boolean hasDatapiece(String id){
        File file = new File(storagePath + "/" + id);
        return file.canRead();
    }

    @Override
    public CompletableFuture<byte[]> getDatapiece(String id) throws IOException {
        if(!hasDatapiece(id)) throw new IOException("Could not find that datapiece " + id);

        AsynchronousFileChannel is = AsynchronousFileChannel.open(Path.of(storagePath + "/" + id), StandardOpenOption.READ);
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        Future<Integer> f = is.read(buffer, 0);
        return CompletableFuture.supplyAsync(() -> {
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
        }, peer.getExecutor());
    }

    /**
     * @brief Deletes a data piece in the backup directory.
     *
     * @param id    data piece identifier
     */
    @Override
    public void deleteDatapiece(String id){
        File datapiece = new File(storagePath + "/" + id);
        datapiece.delete();
    }
}