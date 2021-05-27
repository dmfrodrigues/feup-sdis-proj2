package sdis.Utils;

import java.util.*;

public class FixedSizeList<E> extends LinkedList<E> {
    private final int maxSize;

    public FixedSizeList(int maxSize){
        this.maxSize = maxSize;
    }

    @Override
    public boolean add(E e) {
        if(size() >= maxSize) throw new IllegalStateException("max size reached");
        return super.add(e);
    }

    @Override
    public void add(int index, E element) {
        if(size() >= maxSize) throw new IllegalStateException("max size reached");
        super.add(index, element);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        if(size() >= maxSize) throw new IllegalStateException("max size reached");
        return super.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        if(size() >= maxSize) throw new IllegalStateException("max size reached");
        return super.addAll(index, c);
    }
}
