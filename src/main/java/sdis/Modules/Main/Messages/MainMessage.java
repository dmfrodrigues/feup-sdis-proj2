package sdis.Modules.Main.Messages;

import sdis.Modules.Main.Main;
import sdis.Modules.Message;
import sdis.Peer;
import sdis.Utils.Utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public abstract class MainMessage<T> extends Message {
    public static abstract class Processor extends Message.Processor {
        private final Main main;
        private final SocketChannel socket;

        public Processor(Main main, SocketChannel socket){
            this.main = main;
            this.socket = socket;
        }

        public Main getMain(){
            return main;
        }

        public SocketChannel getSocket(){
            return socket;
        }
    }

    public abstract MainMessage.Processor getProcessor(Peer peer, SocketChannel socket);

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
