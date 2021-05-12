package sdis.Storage;

import sdis.Utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;

/**
 * @brief File chunk iterator.
 *
 * Iterator over chunks of an existing file.
 * Is used to read a file one chunk at a time.
 */
public class FileChunkIterator extends ChunkIterator {
    private final String fileId;
    private final File sourceFile;
    private final String destinationFile;

    /**
     * @brief Construct FileChunkIterator.
     *
     * @param sourceFile      File to parse
     * @param chunkSize Chunk size, in bytes; defaults to 64kB = 64000B
     */
    public FileChunkIterator(File sourceFile, String destinationFile, int chunkSize) throws IOException {
        super(AsynchronousFileChannel.open(sourceFile.toPath(), StandardOpenOption.READ), chunkSize);
        this.sourceFile = sourceFile;
        this.destinationFile = destinationFile;
        fileId = digest();
    }

    private String digest() throws IOException {
        // Create digester
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        assert digest != null;

        // Digest metadata
        String metadata = destinationFile;
        byte[] metadata_bytes = metadata.getBytes();
        digest.update(metadata_bytes, 0, metadata_bytes.length);

        // Digest file contents
        AsynchronousFileChannel inputStream = AsynchronousFileChannel.open(sourceFile.toPath(), StandardOpenOption.READ);
        ByteBuffer buffer = ByteBuffer.allocate(getChunkSize());
        int count, position = 0;
        try {
            while ((count = inputStream.read(buffer, position).get()) > 0) {
                buffer.flip();
                digest.update(buffer);
                buffer.clear();
                position += count;
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new IOException("Failed to read file contents to create file ID");
        }
        inputStream.close();

        byte[] hash = digest.digest();
        return Utils.bytesToHexString(hash);
    }

    public String getFileId(){
        return fileId;
    }
}
