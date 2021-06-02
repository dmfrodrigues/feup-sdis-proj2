package sdis.Modules.Main.Messages;

import sdis.Modules.Main.Main;
import sdis.Modules.Message;
import sdis.Peer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public abstract class MainMessage<T> extends Message {
    public static abstract class Processor extends Message.Processor {
        private final Main main;
        private final Socket socket;

        public Processor(Main main, Socket socket){
            this.main = main;
            this.socket = socket;
        }

        public Main getMain(){
            return main;
        }

        public Socket getSocket(){
            return socket;
        }
    }

    public abstract MainMessage.Processor getProcessor(Peer peer, Socket socket);

    protected abstract byte[] formatResponse(T t);

    protected abstract T parseResponse(byte[] data);

    public T sendTo(InetSocketAddress address) throws IOException, InterruptedException {
        return sendTo(new Socket(address.getAddress(), address.getPort()));
    }

    public T sendTo(Socket socket) throws IOException, InterruptedException {
        socket.getOutputStream().write(this.asByteArray());
        socket.getOutputStream().flush();
        return parseResponse(readAllBytesAndClose(socket));
    }
}
