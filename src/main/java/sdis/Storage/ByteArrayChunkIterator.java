package sdis.Storage;

public class ByteArrayChunkIterator extends ChunkIterator {
    private final byte[] data;

    /**
     * @param data      Data
     * @param chunkSize Chunk size, in bytes
     * @brief Construct ChunkIterator.
     */
    public ByteArrayChunkIterator(byte[] data, int chunkSize) {
        super(chunkSize);
        this.data = data;
    }

    @Override
    public int length() {
        long l = data.length;
        return (int)(l/getChunkSize() + 1);
    }

    long nextIndex = 0;

    @Override
    public boolean hasNext() {
        return nextIndex < length();
    }

    @Override
    public synchronized byte[] next() {
        int offset = (int) ((nextIndex++) * getChunkSize());
        int size = Math.min(data.length - offset, getChunkSize());
        byte[] ret = new byte[size];
        System.arraycopy(data, offset, ret, 0, size);
        return ret;
    }
}
