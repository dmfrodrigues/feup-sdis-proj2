package sdis.Storage;

import sdis.Utils.FixedSizeBuffer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutionException;

/**
 * @brief File chunk output.
 *
 * Is used to reconstruct a file.
 * Is in sync with a local filesystem file.
 * Buffers chunks saved using FileChunkOutput#set(int, byte[]), and writes them to the file whenever possible.
 */
public class FileChunkOutput implements ChunkOutput {
    private final AsynchronousFileChannel fileOutputStream;
    private final int chunkSize;

    /**
     * Create FileChunkOutput.
     *
     * @param file  File to sync with/write to
     * @throws FileNotFoundException If file is not found (never thrown, as file needs not exist)
     */
    public FileChunkOutput(File file, int chunkSize) throws IOException {
        fileOutputStream = AsynchronousFileChannel.open(file.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        this.chunkSize = chunkSize;
    }

    @Override
    public boolean set(long i, byte[] e) {
        byte[] eCopy = new byte[e.length];
        System.arraycopy(e, 0, eCopy, 0, e.length);
        ByteBuffer byteBuffer = ByteBuffer.wrap(eCopy);
        long filePosition = i * chunkSize;
        try {
            fileOutputStream.write(byteBuffer, filePosition).get();
        } catch (InterruptedException | ExecutionException ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public long getMaxIndex() {
        return 1000000000;
    }

    /**
     * Close file.
     *
     * @throws IOException  If fails to close
     */
    public void close() throws IOException {
        fileOutputStream.close();
    }
}
