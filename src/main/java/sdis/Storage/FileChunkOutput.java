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
    private final static int BUFFER_SIZE = 10;

    private final AsynchronousFileChannel fileOutputStream;
    private int filePosition = 0;
    private final FixedSizeBuffer<ByteBuffer> buffer;

    /**
     * Create FileChunkOutput.
     *
     * @param file  File to sync with/write to
     * @throws FileNotFoundException If file is not found (never thrown, as file needs not exist)
     */
    public FileChunkOutput(File file) throws IOException {
        fileOutputStream = AsynchronousFileChannel.open(file.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        buffer = new FixedSizeBuffer<>(BUFFER_SIZE);
    }

    @Override
    public boolean set(long i, byte[] e) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(e);
        buffer.set(i, byteBuffer);
        if(buffer.hasNext()){
            ByteBuffer next = buffer.next();
            try {
                filePosition += fileOutputStream.write(next, filePosition).get();
            } catch (InterruptedException | ExecutionException ex) {
                return false;
            }
        }
        return true;
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
