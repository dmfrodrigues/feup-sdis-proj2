package sdis.Sockets;

import java.nio.*;

public class ByteStuffer {
    private final byte flag;
    private final byte esc;
    private final byte xor;

    public ByteStuffer() {
        this((byte) 0x7E, (byte) 0x7D, (byte) 0x20);
    }

    public ByteStuffer(byte flag, byte esc, byte xor) {
        this.flag = flag;
        this.esc = esc;
        this.xor = xor;
    }

    public ByteBuffer stuff(ByteBuffer unstuffed){
        int size = 0;
        int initialPosition = unstuffed.position();
        while(unstuffed.hasRemaining()){
            byte b = unstuffed.get();
            size += (b == flag || b == esc ? 2 : 1);
        }
        unstuffed.position(initialPosition);

        ByteBuffer stuffed = ByteBuffer.allocate(size);
        while(unstuffed.hasRemaining()){
            byte b = unstuffed.get();
            if(b == flag || b == esc){ stuffed.put(esc); stuffed.put((byte) (b ^ xor)); }
            else stuffed.put(b);
        }
        stuffed.flip();

        return stuffed;
    }

    public void unstuff(ByteBuffer stuffed, ByteBuffer unstuffed){
        stuffed.flip();
        while(stuffed.hasRemaining()){
            byte b = stuffed.get();
            if(b == flag || b == esc) unstuffed.put((byte) (stuffed.get() ^ xor));
            else unstuffed.put(b);
        }
    }
}
