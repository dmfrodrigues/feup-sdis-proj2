package sdis.Modules.DataStorage;

import sdis.UUID;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public abstract class DataStorageAbstract {

    /**
     * @brief Check if the object has a certain datapiece.
     *
     * Returns true if the corresponding call to get(UUID) returned a valid datapiece.
     *
     * @param id    ID of datapiece
     * @return      True if the datapiece is stored, false otherwise
     */
    abstract public Boolean has(UUID id);

    /**
     * @brief Get list of IDs of stored datapieces
     *
     * @return List of IDs of stored datapieces
     */
    abstract public Set<UUID> getAll();

    /**
     * @brief Put a datapiece.
     *
     * @param id    ID of datapiece
     * @param data  Contents of the datapiece
     * @return      A future resolving to a boolean, true if the storing succeeded, false otherwise
     */
    abstract public CompletableFuture<Boolean> put(UUID id, byte[] data);

    /**
     * @brief Get a datapiece.
     *
     * The following absolute guarantees apply:
     *
     * - If a datapiece was put and not deleted in the meanwhile, get yields a valid datapiece
     * - If a datapiece was not put, or if it was put and then deleted, get yields null
     *
     * @param id    ID of datapiece
     * @return      Contents of datapiece, or null if the datapiece does not exist.
     */
    abstract public CompletableFuture<byte[]> get(UUID id);

    abstract public CompletableFuture<Boolean> delete(UUID id);
}
