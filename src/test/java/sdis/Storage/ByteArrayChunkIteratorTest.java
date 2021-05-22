package sdis.Storage;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ByteArrayChunkIteratorTest {

    @Test
    public void test1() throws Exception {
        byte[] data = "my data".getBytes();
        ChunkIterator chunkIterator = new ByteArrayChunkIterator(data, 64000);

        assertEquals(1, chunkIterator.length());
    }
}
