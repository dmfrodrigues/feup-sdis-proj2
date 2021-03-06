package sdis.Modules.DataStorage;

import sdis.Modules.Chord.Chord;
import sdis.UUID;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class DataStorage extends DataStorageAbstract {
    /**
     * Initially reserved storage for backing up chunks (in bytes).
     */
    private static final int INITIAL_STORAGE_SIZE = 1000000000;

    private final LocalDataStorage localDataStorage;
    private final Set<UUID> storedBase = new HashSet<>();
    private final Set<UUID> storedBySuccessor = new HashSet<>();
    private final Chord chord;

    public DataStorage(Path storagePath, Chord chord){
        localDataStorage = new LocalDataStorage(storagePath, INITIAL_STORAGE_SIZE);
        this.chord = chord;
    }

    public void storeBase(UUID id) {
        storedBase.add(id);
    }

    public void unstoreBase(UUID id){
        storedBase.remove(id);
    }

    @Override
    public boolean has(UUID id){
        return storedBase.contains(id);
    }

    @Override
    public Set<UUID> getAll() {
        return storedBase;
    }

    @Override
    public boolean put(UUID id, byte[] data) {
        storeBase(id);
        boolean success = new PutProtocol(chord, this, id, data).invoke();
                if(!success) unstoreBase(id);
                return success;
    }

    public boolean head(UUID id) {
        if(!has(id)) return false;
        return new HeadProtocol(chord, this, id).invoke();
    }

    @Override
    public byte[] get(UUID id) {
        if(!has(id)) return null;
        return new GetProtocol(chord, this, id).invoke();
    }

    @Override
    public boolean delete(UUID id) {
        if(!has(id)) return false;
        unstoreBase(id);
        return new DeleteProtocol(chord, this, id).invoke();
    }

    public LocalDataStorage getLocalDataStorage(){
        return localDataStorage;
    }

    public void registerSuccessorStored(UUID id) {
        storedBySuccessor.add(id);
    }

    public void unregisterSuccessorStored(UUID id) {
        storedBySuccessor.remove(id);
    }

    public boolean successorHasStored(UUID id) {
        return storedBySuccessor.contains(id);
    }

    public Set<UUID> getRedirects() {
        return storedBySuccessor;
    }
}
