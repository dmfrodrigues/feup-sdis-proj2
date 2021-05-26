package sdis.Modules.DataStorage;

import sdis.UUID;

import java.util.Set;

public abstract class DataStorageAbstract {

    /**
     * @brief Check if the object has a certain datapiece.
     *
     * Returns true if the corresponding call to get(UUID) returned a valid datapiece.
     *
     * @param id    ID of datapiece
     * @return      True if the datapiece is stored, false otherwise
     */
    abstract public boolean has(UUID id);

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
    abstract public boolean put(UUID id, byte[] data);

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
    abstract public byte[] get(UUID id);

    /**
     * @brief Delete a datapiece.
     *
     * If a datapiece was not put, of if it was put and already deleted, returns false and does nothing.
     *
     * If a datapiece was put and was successfully deleted, returns true.
     *
     * @param id    UUID of the datapiece to be deleted
     * @return      A future resolving to a boolean, true if the deletion succeeded, false otherwise
     */
    abstract public boolean delete(UUID id);
}
