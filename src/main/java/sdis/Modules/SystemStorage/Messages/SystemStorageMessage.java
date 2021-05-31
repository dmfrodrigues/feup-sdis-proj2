package sdis.Modules.SystemStorage.Messages;

import sdis.Modules.Message;
import sdis.Modules.SystemStorage.SystemStorage;
import sdis.Peer;
import sdis.Utils.Utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public abstract class SystemStorageMessage<T> extends Message {
    public static abstract class Processor extends Message.Processor {
        private final SystemStorage systemStorage;
        private final SocketChannel socket;

        public Processor(SystemStorage systemStorage, SocketChannel socket){
            this.systemStorage = systemStorage;
            this.socket = socket;
        }

        public SystemStorage getSystemStorage(){
            return systemStorage;
        }

        public SocketChannel getSocket(){
            return socket;
        }
    }

    public abstract SystemStorageMessage.Processor getProcessor(Peer peer, SocketChannel socket);

    protected abstract ByteBuffer formatResponse(T t);

    protected abstract T parseResponse(ByteBuffer data);

    public T sendTo(InetSocketAddress address) throws IOException, InterruptedException {
        return sendTo(Utils.createSocket(address));
    }

    public T sendTo(SocketChannel socket) throws IOException, InterruptedException {
        socket.write(this.asByteBuffer());
        return parseResponse(readAllBytesAndClose(socket));
    }
}
