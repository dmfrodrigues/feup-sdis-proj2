package sdis.Storage;

import sdis.Utils.DataBuilder;
import sdis.Utils.FixedSizeBuffer;

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
     */
    public DataBuilderChunkOutput(DataBuilder builder, int bufferSize) {
        this.buffer = new FixedSizeBuffer<>(bufferSize);
        this.builder = builder;
    }

    @Override
    synchronized public boolean set(long i, byte[] e) {
        byte[] eCopy = new byte[e.length];
        System.arraycopy(e, 0, eCopy, 0, e.length);
        if(!buffer.canSet(i)) return false;
        buffer.set(i, eCopy);
        while(buffer.hasNext()){
            byte[] next = buffer.next();
            builder.append(next);
        }
        return true;
    }

    @Override
    synchronized public long getMaxIndex() {
        return buffer.getMaxIndex();
    }
}
