package sdis.Modules;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

public abstract class BlockingTask<T> implements ForkJoinPool.ManagedBlocker {
    private T obj = null;
    private boolean done = false;
    private ExecutionException exception = null;

    protected final void set(T obj){
        this.obj = obj;
    }
    public final T get() throws ExecutionException {
        if(exception != null) throw exception;
        return obj;
    }

    abstract protected void run() throws ExecutionException;

    @Override
    public final boolean block() {
        if(!done){
            try {
                run();
            } catch(ExecutionException e) {
                this.exception = e;
            }
            done = true;
        }
        return true;
    }

    @Override
    public final boolean isReleasable() {
        return done;
    }
}
