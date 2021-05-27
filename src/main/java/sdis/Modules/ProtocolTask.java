package sdis.Modules;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

/**
 * Protocol supplier.
 *
 * Can (and should) throw a ProtocolException when it fails.
 */
public abstract class ProtocolTask<T> extends RecursiveTask<T> {
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
