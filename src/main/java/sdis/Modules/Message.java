package sdis.Modules;

import sdis.Peer;
import sdis.Utils.DataBuilder;

import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.RecursiveAction;

public abstract class Message {
    public final ByteBuffer asByteBuffer(){
        return ByteBuffer.wrap(build().get());
    }

    protected abstract DataBuilder build();

    public static abstract class Processor extends RecursiveAction {}

    public abstract Message.Processor getProcessor(Peer peer, SocketChannel socket);

    protected static ByteBuffer readAllBytesAndClose(SocketChannel socket) throws InterruptedException {
        return ProtocolTask.readAllBytesAndClose(socket);
    }
}
