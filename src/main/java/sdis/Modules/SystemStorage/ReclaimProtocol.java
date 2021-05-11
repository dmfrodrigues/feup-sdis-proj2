package sdis.Modules.SystemStorage;

import sdis.Modules.DataStorage.DataStorage;
import sdis.Modules.DataStorage.LocalDataStorage;
import sdis.Modules.ProtocolSupplier;
import sdis.UUID;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class ReclaimProtocol extends ProtocolSupplier<Void> {
    private static final int MAX_NUMBER_DATAPIECES = 10;

    private SystemStorage systemStorage;

    public ReclaimProtocol(SystemStorage systemStorage){
        this.systemStorage = systemStorage;
    }

    @Override
    public Void get() {
        DataStorage dataStorage = systemStorage.getDataStorage();
        LocalDataStorage localDataStorage = dataStorage.getLocalDataStorage();

        try {
            AtomicInteger expectedSize = new AtomicInteger(localDataStorage.getMemoryUsed().get());
            int capacity = localDataStorage.getCapacity();
            List<UUID> storedLocally = new LinkedList<>(localDataStorage.getAll());
            Collections.shuffle(storedLocally);
            List<CompletableFuture<?>> futuresList = new LinkedList<>();
            while (expectedSize.get() > capacity) {
                if(futuresList.size() >= MAX_NUMBER_DATAPIECES){
                    CompletableFuture<Object> anyOfFuture = CompletableFuture.anyOf(futuresList.toArray(new CompletableFuture[0]));
                    anyOfFuture.get();
                    ListIterator<CompletableFuture<?>> it = futuresList.listIterator();
                    while(it.hasNext()){
                        CompletableFuture<?> f = it.next();
                        if(f.isDone()){
                            f.get();
                            it.remove();
                        }
                    }
                }

                UUID id = storedLocally.get(0);
                storedLocally.remove(0);

                CompletableFuture<Boolean> f = localDataStorage.get(id)
                .thenComposeAsync((byte[] data) -> {
                    expectedSize.addAndGet(-data.length);
                    return systemStorage.delete(id)
                        .thenCompose((Boolean deleteSuccessful) -> {
                            if (!deleteSuccessful) {
                                return localDataStorage.delete(id)
                                    .thenApply((Boolean localDeleteSuccessful) -> {
                                        if (!localDeleteSuccessful)
                                            throw new CompletionException(new IOException("Failed to delete datapiece " + id));
                                        return false;
                                    });
                            }
                            return systemStorage.put(id, data);
                        });
                });
                futuresList.add(f);
            }

            if(localDataStorage.getMemoryUsed().get() > capacity) return get();
        } catch (InterruptedException e) {
            throw new CompletionException(e);
        } catch (ExecutionException e) {
            throw new CompletionException(e.getCause());
        }

        return null;
    }
}
