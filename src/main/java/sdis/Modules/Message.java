package sdis.Modules;

import sdis.Peer;
import sdis.Utils.DataBuilder;

import java.net.Socket;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

public abstract class Message {
    public final byte[] asByteArray(){
        return build().get();
    }

    protected abstract DataBuilder build();

    public static abstract class Processor implements Supplier<Void> {}

    public abstract Message.Processor getProcessor(Peer peer, Socket socket);

    protected static void readAllBytesAndClose(Socket socket) throws InterruptedException {
        ProtocolTask.readAllBytesAndClose(socket);
    }
}
