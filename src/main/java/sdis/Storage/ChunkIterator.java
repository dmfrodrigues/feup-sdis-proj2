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
    byte[] buffer;
    AsynchronousFileChannel fileStream;

    /**
     * @brief Construct ChunkIterator.
     *
     * @param fileStream    Asynchronous file channel to read from
     * @param chunkSize     Chunk size, in bytes
     */
    public ChunkIterator(AsynchronousFileChannel fileStream, int chunkSize) throws IOException {
        this.fileStream = fileStream;
        this.chunkSize = chunkSize;
        buffer = new byte[this.chunkSize];
    }

    public final int getChunkSize(){
        return chunkSize;
    }

    abstract public String getFileId();

    /**
     * Length of chunked file, in chunks.
     *
     * If the file size is an exact multiple of the chunk size, an extra empty chunk is considered at the end.
     *
     * @return  Length of chunked file, in chunks
     */
    public final long length() throws IOException {
        long l = fileStream.size();
        return l/chunkSize + 1;
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
        ByteBuffer buffer = ByteBuffer.allocate(chunkSize);
        Future<Integer> f = fileStream.read(buffer, nextIndex*chunkSize);
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
