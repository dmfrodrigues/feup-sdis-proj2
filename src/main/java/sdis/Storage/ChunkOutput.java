package sdis.Storage;

/**
 * @brief Chunk output.
 *
 * Is used to reconstruct a file.
 * Is in sync with a local filesystem file.
 * Buffers chunks saved using FileChunkOutput#set(int, byte[]), and writes them to the file whenever possible.
 */
public interface ChunkOutput {
    /**
     * @brief Add a chunk.
     *
     * Fails if the chunk index is too far ahead of the first missing chunk.
     *
     * @param i     Index of the chunk in the file
     * @param e     Chunk
     */
    boolean set(long i, byte[] e);

    /**
     * @brief Get maximum index that can be used in set(long, byte[]).
     *
     * @return Maximum index that can be set
     */
    long getMaxIndex();
}
