package sdis.Storage;

import java.io.File;
import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.StandardOpenOption;

/**
 * @brief File chunk iterator.
 *
 * Iterator over chunks of an existing file.
 * Is used to read a file one chunk at a time.
 */
public class FileChunkIterator extends AsynchronousChunkIterator {
    /**
     * @brief Construct FileChunkIterator.
     *
     * @param sourceFile      File to parse
     * @param chunkSize Chunk size, in bytes; defaults to 64kB = 64000B
     */
    public FileChunkIterator(File sourceFile, int chunkSize) throws IOException {
        super(AsynchronousFileChannel.open(sourceFile.toPath(), StandardOpenOption.READ), chunkSize);
    }
}
