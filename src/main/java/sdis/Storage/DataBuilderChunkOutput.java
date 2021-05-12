package sdis.Storage;

import sdis.Utils.DataBuilder;
import sdis.Utils.FixedSizeBuffer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutionException;

/**
 * @brief Data builder chunk output.
 *
 * Is used to reconstruct a file.
 * Is in sync with a local filesystem file.
 * Buffers chunks saved using FileChunkOutput#set(int, byte[]), and writes them to the file whenever possible.
 */
public class DataBuilderChunkOutput implements ChunkOutput {
    private final FixedSizeBuffer<byte[]> buffer;
    private final DataBuilder builder;

    /**
     * Create FileChunkOutput.
     *
     * @param builder  Data builder to write to
     * @throws FileNotFoundException If file is not found (never thrown, as file needs not exist)
     */
    public DataBuilderChunkOutput(DataBuilder builder, int bufferSize) {
        this.buffer = new FixedSizeBuffer<>(bufferSize);
        this.builder = builder;
    }

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
    @Override
    public boolean set(long i, byte[] e) {
        byte[] eCopy = new byte[e.length];
        System.arraycopy(e, 0, eCopy, 0, e.length);
        try {
            buffer.set(i, eCopy);
        } catch(ArrayIndexOutOfBoundsException ex){
            return false;
        }
        if(buffer.hasNext()){
            byte[] next = buffer.next();
            builder.append(next);
        }
        return true;
    }
}
