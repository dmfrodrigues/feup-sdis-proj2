package sdis.Storage;

import sdis.Peer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Future;

/**
 * @brief Chunk iterator.
 *
 * Iterator over chunks.
 * Is used to read a file one chunk at a time.
 */
abstract public class AsynchronousChunkIterator extends ChunkIterator {
    private final AsynchronousFileChannel fileStream;

    /**
     * @brief Construct ChunkIterator.
     *
     * @param fileStream    Asynchronous file channel to read from
     * @param chunkSize     Chunk size, in bytes
     */
    public AsynchronousChunkIterator(AsynchronousFileChannel fileStream, int chunkSize) {
        super(chunkSize);
        this.fileStream = fileStream;
    }

    /**
     * Length of chunked file, in chunks.
     *
     * If the file size is an exact multiple of the chunk size, an extra empty chunk is considered at the end.
     *
     * @return  Length of chunked file, in chunks
     */
    @Override
    public final int length() throws IOException {
        long l = fileStream.size();
        return (int)(l/getChunkSize() + 1);
    }

    long nextIndex = 0;

    @Override
    public final synchronized boolean hasNext() {
        try {
            return nextIndex < length();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public final synchronized CompletableFuture<byte[]> next() {
        ByteBuffer buffer = ByteBuffer.allocate(getChunkSize());
        Future<Integer> f = fileStream.read(buffer, nextIndex*getChunkSize());
        ++nextIndex;
        return CompletableFuture.supplyAsync(() -> {
            try {
                int size = f.get();
                byte[] bufferArray = new byte[size];
                buffer.flip();
                buffer.get(bufferArray, 0, size);
                return bufferArray;
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, Peer.getExecutor());
    }

    public final void close() throws IOException {
        fileStream.close();
    }
}
