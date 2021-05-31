package sdis.Modules;

import java.io.IOException;
import java.net.Socket;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

/**
 * Protocol supplier.
 *
 * Can (and should) throw a ProtocolException when it fails.
 */
public abstract class ProtocolTask<T> extends RecursiveTask<T> {
    private static final int PARALLELISM_LEVEL = 6;

    public static void invokeTasks(List<ProtocolTask<?>> tasks) {
        Queue<ProtocolTask<?>> q = new LinkedList<>();
        for(ProtocolTask<?> task: tasks){
            if(q.size() >= PARALLELISM_LEVEL) {
                q.remove().join();
            }

            task.fork();
            q.add(task);
        }

        while(!q.isEmpty()){
            q.remove().join();
        }
    }

    public static boolean reduceTasks(Collection<ProtocolTask<Boolean>> tasks){
        return tasks.stream().map((RecursiveTask<Boolean> task) -> {
            try {
                return task.get();
            } catch (InterruptedException | ExecutionException e) {
                return false;
            }
        }).reduce((Boolean a, Boolean b) -> a && b).orElse(true);
    }

    static public boolean invokeAndReduceTasks(Collection<ProtocolTask<Boolean>> tasks) {
        invokeTasks(tasks.stream().map((ProtocolTask<Boolean> task) -> (ProtocolTask<?>)task).collect(Collectors.toList()));
        return reduceTasks(tasks);
    }

    private static class ReadAllBytesFromSocketTask extends BlockingTask<byte[]> {
        private final Socket socket;

        public ReadAllBytesFromSocketTask(Socket socket){
            this.socket = socket;
        }

        @Override
        protected void run() throws ExecutionException {
            try {
                byte[] data = socket.getInputStream().readAllBytes();
                set(data);
            } catch (IOException e) {
                throw new ExecutionException(e);
            }
        }
    }

    protected static byte[] readAllBytes(Socket socket) throws InterruptedException {
        ReadAllBytesFromSocketTask task = new ReadAllBytesFromSocketTask(socket);
        ForkJoinPool.managedBlock(task);
        try {
            return task.get();
        } catch (ExecutionException e) {
            throw new CompletionException(e.getCause());
        }
    }

    protected static byte[] readAllBytesAndClose(Socket socket) throws InterruptedException {
        try {
            socket.shutdownOutput();
            byte[] response = readAllBytes(socket);
            socket.close();
            return response;
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }
}
