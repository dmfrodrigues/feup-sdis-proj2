package sdis.Storage;

import java.io.IOException;

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
     * @throws IOException                      If write to file fails
     * @throws ArrayIndexOutOfBoundsException   If chunk index was not accepted
     */
    boolean set(long i, byte[] e);
}
