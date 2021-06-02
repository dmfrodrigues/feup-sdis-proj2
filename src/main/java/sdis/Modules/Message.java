package sdis.Modules;

import sdis.Peer;
import sdis.Utils.DataBuilder;

import java.net.Socket;
import java.util.concurrent.RecursiveAction;

public abstract class Message {
    public final byte[] asByteArray(){
        return build().get();
    }

    protected abstract DataBuilder build();

    public static abstract class Processor extends RecursiveAction {}

    public abstract Message.Processor getProcessor(Peer peer, Socket socket);

    protected static byte[] readAllBytesAndClose(Socket socket) throws InterruptedException {
        return ProtocolTask.readAllBytesAndClose(socket);
    }
}
