package sdis.Modules.SystemStorage;

import sdis.Modules.DataStorage.DataStorage;
import sdis.Modules.DataStorage.LocalDataStorage;
import sdis.Modules.ProtocolTask;
import sdis.UUID;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public class ReclaimProtocol extends ProtocolTask<Void> {
    private static final int MAX_NUMBER_DATAPIECES = 10;

    private final SystemStorage systemStorage;

    public ReclaimProtocol(SystemStorage systemStorage){
        this.systemStorage = systemStorage;
    }

    @Override
    public Void compute() {
        DataStorage dataStorage = systemStorage.getDataStorage();
        LocalDataStorage localDataStorage = dataStorage.getLocalDataStorage();

        try {
            long expectedSize = localDataStorage.getMemoryUsed();
            int capacity = localDataStorage.getCapacity();
            List<UUID> storedLocally = new LinkedList<>(localDataStorage.getAll());
            Collections.shuffle(storedLocally);
            List<ProtocolTask<Boolean>> tasks = new LinkedList<>();
            while (expectedSize > capacity) {
                UUID id = storedLocally.get(0);
                storedLocally.remove(0);

                long datapieceSize = localDataStorage.getSize(id);
                expectedSize -= datapieceSize;

                tasks.add(new ProtocolTask<>() {
                    @Override
                    protected Boolean compute() {
                        byte[] data = localDataStorage.get(id);
                        boolean deleteSuccessful = systemStorage.delete(id);
                        if (!deleteSuccessful) {
                            boolean localDeleteSuccessful = localDataStorage.delete(id);
                            if (!localDeleteSuccessful)
                                throw new CompletionException(new IOException("Failed to delete datapiece " + id));
                            return false;
                        }
                        return systemStorage.put(id, data);
                    }
                });
            }

            invokeAll(tasks);

            if(localDataStorage.getMemoryUsed() > capacity) return get();
        } catch (InterruptedException e) {
            throw new CompletionException(e);
        } catch (ExecutionException e) {
            throw new CompletionException(e.getCause());
        }

        return null;
    }
}
