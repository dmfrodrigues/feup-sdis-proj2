package sdis.Storage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
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

    private static class ReadBlocker implements ForkJoinPool.ManagedBlocker {

        private final Future<Integer> f;
        private final ByteBuffer buffer;
        private byte[] bufferArray = null;

        public ReadBlocker(Future<Integer> f, ByteBuffer buffer){
            this.f = f;
            this.buffer = buffer;
        }

        @Override
        public boolean block() {
            try {
                int size = f.get();
                bufferArray = new byte[size];
                buffer.flip();
                buffer.get(bufferArray, 0, size);
                return true;
            } catch (ExecutionException | InterruptedException e) {
                throw new CompletionException(e);
            }
        }

        @Override
        public boolean isReleasable() {
            return (bufferArray != null);
        }
    }

    @Override
    public final synchronized byte[] next() {
        ByteBuffer buffer;
        Future<Integer> f;
        synchronized(this) {
            buffer = ByteBuffer.allocate(getChunkSize());
            f = fileStream.read(buffer, nextIndex * getChunkSize());
            ++nextIndex;
        }
        ReadBlocker readBlocker = new ReadBlocker(f, buffer);
        try {
            ForkJoinPool.managedBlock(readBlocker);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
        return readBlocker.bufferArray;
    }

    public final void close() throws IOException {
        fileStream.close();
    }
}
