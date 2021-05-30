package sdis.Modules.DataStorage;

import sdis.UUID;
import sdis.Utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Manage peer chunk storage.
 *
 * There should be at most one instance of this class.
 */
public class LocalDataStorage extends DataStorageAbstract {
    private static final int BUFFER_SIZE = 80000;

    private final Path storagePath;
    private int capacity;

    public LocalDataStorage(Path storagePath, int capacity){
        this.storagePath = storagePath;
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
    public long getMemoryUsed(){
        File storage = storagePath.toFile();
        long size = 0;
        for (File file : Objects.requireNonNull(storage.listFiles()))
            size += file.length();
        return size;
    }

    public boolean canPut(int length){
        return (getMemoryUsed() + length <= getCapacity());
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

    public boolean has(UUID id){
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
    public boolean put(UUID id, byte[] data) {
            boolean canPut = canPut(data.length);
            if(!canPut) return false;
            ByteBuffer buffer = ByteBuffer.wrap(data);
            try {
                String pathStr = storagePath + "/" + id;
                File pathFile = new File(pathStr);
                if(!pathFile.getParentFile().exists() && !pathFile.getParentFile().mkdirs()) return false;
                AsynchronousFileChannel os = AsynchronousFileChannel.open(Path.of(pathStr), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                os.write(buffer, 0).get();
                os.close();
            } catch (InterruptedException | ExecutionException | IOException e) {
                e.printStackTrace();
                return false;
            }
            return true;
    }

    @Override
    public byte[] get(UUID id) {
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
    }

    /**
     * @brief Deletes a data piece in the backup directory.
     *
     * @param id    data piece identifier
     */
    @Override
    public boolean delete(UUID id){
        File datapiece = new File(storagePath + "/" + id);
        return datapiece.delete();
    }

    public long getSize(UUID id) {
        File file = new File(storagePath + "/" + id);
        return file.length();
    }

    public Path getStoragePath() {
        return storagePath;
    }
}