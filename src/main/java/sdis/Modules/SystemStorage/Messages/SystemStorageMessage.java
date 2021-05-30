package sdis.Modules.SystemStorage.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Modules.Message;
import sdis.Modules.SystemStorage.SystemStorage;
import sdis.Peer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public abstract class SystemStorageMessage<T> extends Message {
    public static abstract class Processor extends Message.Processor {
        private final SystemStorage systemStorage;
        private final Socket socket;

        public Processor(SystemStorage systemStorage, Socket socket){
            this.systemStorage = systemStorage;
            this.socket = socket;
        }

        public SystemStorage getSystemStorage(){
            return systemStorage;
        }

        public Socket getSocket(){
            return socket;
        }
    }

    public abstract SystemStorageMessage.Processor getProcessor(Peer peer, Socket socket);

    protected abstract byte[] formatResponse(T t);

    protected abstract T parseResponse(byte[] data);

    public T sendTo(InetSocketAddress address) throws IOException, InterruptedException {
        return sendTo(new Socket(address.getAddress(), address.getPort()));
    }

    public T sendTo(Socket socket) throws IOException, InterruptedException {
        socket.getOutputStream().write(this.asByteArray());
        return parseResponse(readAllBytesAndClose(socket));
    }
}
