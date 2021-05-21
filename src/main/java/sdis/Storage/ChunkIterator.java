package sdis.Storage;

import sdis.Peer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Future;

/**
 * @brief Chunk iterator.
 *
 * Iterator over chunks.
 * Is used to read a file one chunk at a time.
 */
abstract public class ChunkIterator implements Iterator<CompletableFuture<byte[]>> {
    private final int chunkSize;

    /**
     * @brief Construct ChunkIterator.
     *
     * @param chunkSize     Chunk size, in bytes
     */
    public ChunkIterator(int chunkSize) throws IOException {
        this.chunkSize = chunkSize;
    }

    public final int getChunkSize(){
        return chunkSize;
    }

    /**
     * Length of chunked file, in chunks.
     *
     * If the file size is an exact multiple of the chunk size, an extra empty chunk is considered at the end.
     *
     * @return  Length of chunked file, in chunks
     */
    abstract public long length() throws IOException;

    abstract public boolean hasNext();

    abstract public CompletableFuture<byte[]> next();
}
