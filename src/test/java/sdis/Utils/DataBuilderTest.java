package sdis.Utils;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertArrayEquals;

public class DataBuilderTest {

    @Test
    public void test1() throws Exception {
        DataBuilder b = new DataBuilder("abcde".getBytes());

        assertArrayEquals("abcde".getBytes(), b.get());
    }

    @Test
    public void test2() throws Exception {
        DataBuilder b = new DataBuilder("The quick".getBytes());
        b.append(" brown fox".getBytes());

        System.out.println(new String(b.get()));

        assertArrayEquals("The quick brown fox".getBytes(), b.get());
    }

    @Test
    public void test3() throws Exception {
        DataBuilder b = new DataBuilder("The quick".getBytes());
        b.append(" brown fox".getBytes());
        b.append(new DataBuilder(" jumps over".getBytes()));

        assertArrayEquals("The quick brown fox jumps over".getBytes(), b.get());
    }

    @Test
    public void test4() throws Exception {
        DataBuilder fox = new DataBuilder(" fox".getBytes());
        DataBuilder theQuickBrownFox = new DataBuilder("The".getBytes()).append(" quick".getBytes()).append(new DataBuilder(" brown".getBytes())).append(fox);
        DataBuilder jumpsOver = new DataBuilder(" jumps over".getBytes());
        DataBuilder theLazyDog = new DataBuilder(" the lazy dog".getBytes());
        DataBuilder action = new DataBuilder();
        action.append(jumpsOver).append(theLazyDog);
        DataBuilder sentence = new DataBuilder();
        sentence.append(theQuickBrownFox).append(action);

        assertArrayEquals("The quick brown fox jumps over the lazy dog".getBytes(), sentence.get());
    }

}
