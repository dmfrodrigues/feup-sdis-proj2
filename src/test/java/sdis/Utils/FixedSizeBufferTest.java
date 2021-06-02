package sdis.Utils;

import org.junit.Test;
import sdis.Storage.ChunkOutput;
import sdis.Storage.DataBuilderChunkOutput;

import static org.junit.Assert.*;

public class FixedSizeBufferTest {

    @Test
    public void test1() {
        FixedSizeBuffer<Integer> buffer = new FixedSizeBuffer<>(10);

        assertTrue(buffer.canSet(0));
        buffer.set(0, 0);
        assertTrue(buffer.hasNext());
        assertEquals(Integer.valueOf(0), buffer.next());

        assertTrue(buffer.canSet(1));
        buffer.set(1, 1);
        assertTrue(buffer.hasNext());
        assertEquals(Integer.valueOf(1), buffer.next());
    }

    @Test
    public void test2() {
        FixedSizeBuffer<Integer> buffer = new FixedSizeBuffer<>(10);

        assertTrue(buffer.canSet(1));
        buffer.set(1, 1);
        assertFalse(buffer.hasNext());

        assertTrue(buffer.canSet(0));
        buffer.set(0, 0);

        assertTrue(buffer.hasNext());
        assertEquals(Integer.valueOf(0), buffer.next());
        assertTrue(buffer.hasNext());
        assertEquals(Integer.valueOf(1), buffer.next());
    }
}
