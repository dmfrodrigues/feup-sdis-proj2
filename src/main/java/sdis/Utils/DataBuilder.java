package sdis.Utils;

import java.util.LinkedList;
import java.util.List;

public class DataBuilder {

    private final byte[] data;
    private final List<DataBuilder> parts = new LinkedList<>();

    public DataBuilder(){
        this.data = new byte[0];
    }

    public DataBuilder(byte[] b){
        this.data = b;
    }

    public DataBuilder(byte b){
        this(new byte[]{ b });
    }

    public DataBuilder append(DataBuilder b){
        parts.add(b);
        return this;
    }

    public DataBuilder append(byte[] b){
        return append(new DataBuilder(b));
    }

    public DataBuilder append(byte b){
        return append(new DataBuilder(b));
    }

    public int size(){
        int sz = 0;
        for(DataBuilder d: parts) sz += d.size();
        sz += data.length;
        return sz;
    }

    private int copyTo(byte[] dest, int initialOffset){
        int offset = initialOffset;

        System.arraycopy(data, 0, dest, offset, data.length);
        offset += data.length;

        for(DataBuilder b: parts){
            int size = b.copyTo(dest, offset);
            offset += size;
        }

        return offset - initialOffset;
    }

    public byte[] get(){
        byte[] ret = new byte[size()];
        copyTo(ret, 0);
        return ret;
    }
}
