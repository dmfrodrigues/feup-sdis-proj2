package sdis.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Fixed-size buffer.
 *
 * This class works like a fixed-size queue, which has a maximum size. If the difference between the indexes of the
 * first and last elements is larger than the buffer size, it will throw an exception.
 *
 * @param <T>
 */
public class FixedSizeBuffer<T> {
    private final List<T> queue;
    private final boolean[] ready;
    /**
     * Index of the first available element.
     * Does not wrap when it is larger than queue.size().
     */
    private long begin = 0;

    public FixedSizeBuffer(int size){
        queue = new ArrayList<>(size);
        for(int i = 0; i < size; ++i) queue.add(null);
        ready = new boolean[size];
        Arrays.fill(ready, false);
    }

    /**
     * Get index of the first element in the queue.
     *
     * @return  Index of first element in queue
     */
    synchronized public long getBegin(){
        return begin;
    }

    /**
     * Get maximum index that can be set.
     * 
     * @return  Maximum index that can be set
     */
    synchronized public long getMaxIndex() {
        return begin+queue.size()-1;
    }

    /**
     * Check if an element can be inserted at index i.
     *
     * @param i     Index in the queue
     * @return      True if an element can be inserted at that index, false otherwise
     */
    synchronized public boolean canSet(long i){
        return (i < begin+queue.size());
    }

    /**
     * Set element at a certain index.
     *
     * If index is less than begin, it is ignored.
     *
     * @param i     Index in the queue
     * @param e     Element to insert
     */
    synchronized public void set(long i, T e) throws ArrayIndexOutOfBoundsException {
        if(i < begin) return;
        if(!canSet(i)) throw new ArrayIndexOutOfBoundsException();
        int idx = (int) (i % queue.size());
        ready[idx] = true;
        queue.set(idx, e);
    }

    /**
     * Check if next element can be extracted.
     *
     * @return  True if the queue already has the next element to be extracted, false otherwise
     */
    synchronized public boolean hasNext(){
        int idx = (int) (begin % queue.size());
        return ready[idx];
    }

    /**
     * Extract next element.
     *
     * The next element has as its index the number returned by FixedSizeBuffer#getBegin().
     *
     * @return  The next element
     */
    synchronized public T next(){
        if(!hasNext()) throw new IllegalStateException();
        int idx = (int) (begin % queue.size());
        T ret = queue.get(idx);
        ready[idx] = false;
        begin++;
        return ret;
    }
}
