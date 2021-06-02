package sdis.Utils;

import org.junit.Test;
import sdis.Storage.ChunkOutput;
import sdis.Storage.DataBuilderChunkOutput;

import static org.junit.Assert.assertArrayEquals;

public class ChunkOutputTest {

    @Test
    public void test1() {
        DataBuilder builder = new DataBuilder();
        ChunkOutput chunkOutput = new DataBuilderChunkOutput(builder, 10);

        chunkOutput.set(0, Integer.toString(0).getBytes());
        chunkOutput.set(1, Integer.toString(1).getBytes());

        assertArrayEquals("01".getBytes(), builder.get());
    }

    @Test
    public void test2() {
        DataBuilder builder = new DataBuilder();
        DataBuilderChunkOutput chunkOutput = new DataBuilderChunkOutput(builder, 10);

        chunkOutput.set(1, Integer.toString(1).getBytes());
        chunkOutput.set(0, Integer.toString(0).getBytes());

        assertArrayEquals("01".getBytes(), builder.get());
    }

    @Test
    public void test3() {
        DataBuilder builder = new DataBuilder();
        ChunkOutput chunkOutput = new DataBuilderChunkOutput(builder, 10);

        chunkOutput.set(0, Integer.toString(0).getBytes());
        chunkOutput.set(3, Integer.toString(3).getBytes());
        chunkOutput.set(2, Integer.toString(2).getBytes());
        chunkOutput.set(4, Integer.toString(4).getBytes());
        chunkOutput.set(1, Integer.toString(1).getBytes());

        assertArrayEquals("01234".getBytes(), builder.get());
    }
}
