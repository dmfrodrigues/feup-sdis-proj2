package sdis.Storage;

import sdis.Peer;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class ByteArrayChunkIterator extends ChunkIterator {
    private byte[] data;

    /**
     * @param data      Data
     * @param chunkSize Chunk size, in bytes
     * @brief Construct ChunkIterator.
     */
    public ByteArrayChunkIterator(byte[] data, int chunkSize) throws IOException {
        super(chunkSize);
        this.data = data;
    }

    @Override
    public long length() {
        return data.length;
    }

    long nextIndex = 0;

    @Override
    public boolean hasNext() {
        return nextIndex < length();
    }

    @Override
    public synchronized CompletableFuture<byte[]> next() {
        int offset = (int) nextIndex*getChunkSize();
        int size = Math.min(data.length - offset, getChunkSize());
        return CompletableFuture.supplyAsync(() -> {
            byte[] ret = new byte[size];
            System.arraycopy(data, offset, ret, 0, size);
            return ret;
        }, Peer.getExecutor());
    }
}
